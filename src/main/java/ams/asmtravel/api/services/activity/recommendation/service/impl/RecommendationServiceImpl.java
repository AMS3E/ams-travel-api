package ams.asmtravel.api.services.activity.recommendation.service.impl;

import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.activity.recommendation.config.RecommendationProperties;
import ams.asmtravel.api.services.activity.recommendation.domain.RankedDestination;
import ams.asmtravel.api.services.activity.recommendation.domain.RecommendationReason;
import ams.asmtravel.api.services.activity.recommendation.domain.Taste;
import ams.asmtravel.api.services.activity.recommendation.dto.request.RecommendationRequest;
import ams.asmtravel.api.services.activity.recommendation.dto.response.RecommendationResponse;
import ams.asmtravel.api.services.activity.recommendation.mapper.RecommendationMapper;
import ams.asmtravel.api.services.activity.recommendation.repository.BrowseHistoryRepository;
import ams.asmtravel.api.services.activity.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Recommended, from published places and browsing. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationServiceImpl implements RecommendationService {

    private final BrowseHistoryRepository browseHistory;
    private final DestinationService destinationService;
    private final RecommendationMapper mapper;
    private final RecommendationProperties properties;

    @Override
    public RecommendationResponse forVisitor(UUID userId, RecommendationRequest request) {
        int size = request.limit() == null ? properties.size() : request.limit();
        Map<UUID, DestinationSummary> catalogue = new LinkedHashMap<>();
        destinationService.findPublishedSummaries().forEach(place -> catalogue.put(place.id(), place));
        Map<UUID, Long> browses = browseHistory.recentBrowses();
        Set<UUID> browsed = userId == null ? Set.of() : browseHistory.browsedBy(userId);
        Taste taste = tasteOf(browsed, catalogue);
        List<RankedDestination> ranked = new ArrayList<>();

        if (taste.isEmpty()) {
            for (Map.Entry<UUID, Long> entry : browses.entrySet()) {
                if (ranked.size() >= size) {
                    break;
                }
                if (entry.getValue() > 0 && catalogue.containsKey(entry.getKey())) {
                    ranked.add(new RankedDestination(entry.getKey(), entry.getValue(), RecommendationReason.MOST_BROWSED));
                }
            }
            return mapper.toResponse(false, ranked);
        }

        // Somewhere new to them, most like what they browse, then most browsed by everyone.
        Comparator<DestinationSummary> order = Comparator
                .comparingInt((DestinationSummary place) -> match(taste, place)).reversed()
                .thenComparing(Comparator.comparingLong((DestinationSummary place) -> browses.getOrDefault(place.id(), 0L))
                        .reversed())
                .thenComparing(DestinationSummary::name);
        catalogue.values().stream()
                .filter(place -> !browsed.contains(place.id()))
                .filter(place -> match(taste, place) > 0)
                .sorted(order)
                .limit(size)
                .map(place -> new RankedDestination(place.id(), browses.getOrDefault(place.id(), 0L),
                        RecommendationReason.LIKE_YOUR_BROWSING))
                .forEach(ranked::add);
        return mapper.toResponse(true, ranked);
    }

    private static Taste tasteOf(Set<UUID> browsed, Map<UUID, DestinationSummary> catalogue) {
        Set<UUID> categories = new HashSet<>();
        Set<UUID> interests = new HashSet<>();
        Set<UUID> regions = new HashSet<>();
        for (UUID id : browsed) {
            DestinationSummary place = catalogue.get(id);
            if (place != null) {
                categories.addAll(place.categoryIds());
                interests.addAll(place.interestIds());
                if (place.regionId() != null) {
                    regions.add(place.regionId());
                }
            }
        }
        return new Taste(categories, interests, regions);
    }

    private static int match(Taste taste, DestinationSummary place) {
        return taste.match(place.categoryIds(), place.interestIds(), place.regionId());
    }
}
