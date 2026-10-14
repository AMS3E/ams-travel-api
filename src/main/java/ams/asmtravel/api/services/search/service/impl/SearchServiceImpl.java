package ams.asmtravel.api.services.search.service.impl;

import ams.asmtravel.api.services.corridor.domain.CorridorStatus;
import ams.asmtravel.api.services.corridor.dto.response.CorridorResponse;
import ams.asmtravel.api.services.corridor.service.CorridorService;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSearch;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.destination.dto.response.DestinationResponse;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.destination.food.dto.response.FoodResponse;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.stay.dto.response.StayResponse;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.location.province.domain.ProvinceSummary;
import ams.asmtravel.api.services.location.province.service.ProvinceService;
import ams.asmtravel.api.services.location.region.domain.RegionKind;
import ams.asmtravel.api.services.location.region.service.RegionService;
import ams.asmtravel.api.services.review.review.domain.ReviewTargetType;
import ams.asmtravel.api.services.review.review.dto.response.ReviewSummaryResponse;
import ams.asmtravel.api.services.review.review.service.ReviewService;
import ams.asmtravel.api.services.search.domain.SearchType;
import ams.asmtravel.api.services.search.domain.SuggestionKind;
import ams.asmtravel.api.services.search.dto.request.SearchRequest;
import ams.asmtravel.api.services.search.dto.response.ProvinceGroupResponse;
import ams.asmtravel.api.services.search.dto.response.SearchHitResponse;
import ams.asmtravel.api.services.search.dto.response.SuggestionResponse;
import ams.asmtravel.api.services.search.mapper.SearchMapper;
import ams.asmtravel.api.services.search.repository.NameLookupRepository;
import ams.asmtravel.api.services.search.service.SearchService;
import ams.asmtravel.api.services.verification.domain.TravelerSealBar;
import ams.asmtravel.api.services.verification.dto.response.VerificationResponse;
import ams.asmtravel.api.services.verification.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Destinations are searched in the database, a page at a time. Stays, food places and corridors are few,
 * so their published lists are filtered here; the same cards come back either way. Around a destination
 * ({@code near}) the order is nearest first instead of A to Z.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private static final int MAX_PAGE = 100;
    private static final int SUMMARY_BATCH = 100;
    /** Attractions around a destination: how far out, and at most how many. */
    private static final double NEAR_RADIUS_KM = 25;
    private static final int NEAR_LIMIT = 100;

    private final DestinationService destinationService;
    private final StayService stayService;
    private final FoodService foodService;
    private final CorridorService corridorService;
    private final RegionService regionService;
    private final ProvinceService provinceService;
    private final ReviewService reviewService;
    private final VerificationService verificationService;
    private final NameLookupRepository names;
    private final SearchMapper mapper;

    @Override
    public Page<SearchHitResponse> search(SearchRequest request, Pageable pageable) {
        PageRequest page = PageRequest.of(pageable.getPageNumber(), Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE));
        return switch (request.typeOrDefault()) {
            case DESTINATION -> {
                if (request.near() != null) {
                    yield paged(destinationsNear(request), page, this::destinationHits);
                }
                Page<DestinationResponse> found = destinationService.searchPublished(criteria(request),
                        page.withSort(Sort.by("name").ascending()));
                yield new PageImpl<>(destinationHits(found.getContent()), page, found.getTotalElements());
            }
            case STAY -> paged(stays(request), page, this::stayHits);
            case FOOD -> paged(foods(request), page, this::foodHits);
            case CORRIDOR -> paged(corridors(request), page, this::corridorHits);
        };
    }

    @Override
    public List<ProvinceGroupResponse> byProvince(SearchRequest request, int perProvince) {
        Map<UUID, String> provinceNames = names.provinceNames();
        return switch (request.typeOrDefault()) {
            case DESTINATION -> grouped(request.near() != null ? destinationsNear(request)
                            : destinationService.searchPublished(criteria(request), Pageable.unpaged(Sort.by("name").ascending())).getContent(),
                    place -> one(place.provinceId()), this::destinationHits, perProvince, provinceNames);
            case STAY -> grouped(stays(request), stay -> one(stay.provinceId()), this::stayHits, perProvince, provinceNames);
            case FOOD -> grouped(foods(request), food -> one(food.provinceId()), this::foodHits, perProvince, provinceNames);
            case CORRIDOR -> grouped(corridors(request), CorridorResponse::stopProvinceIds, this::corridorHits, perProvince, provinceNames);
        };
    }

    @Override
    public List<SuggestionResponse> suggest(String text, int limit) {
        String needle = text == null ? "" : text.trim();
        if (needle.isEmpty() || limit < 1) {
            return List.of();
        }
        List<SuggestionResponse> found = new ArrayList<>();
        destinationService.suggestPublished(needle, limit)
                .forEach(place -> found.add(new SuggestionResponse(SuggestionKind.DESTINATION, place.id(), place.name(), place.slug())));
        provinceService.findSummaries().stream().filter(province -> contains(province.name(), needle))
                .forEach(province -> found.add(new SuggestionResponse(SuggestionKind.PROVINCE, province.id(), province.name(), province.slug())));
        regionService.getAll(RegionKind.TOURISM).stream()
                .filter(region -> !Boolean.FALSE.equals(region.isActive()) && contains(region.name(), needle))
                .forEach(region -> found.add(new SuggestionResponse(SuggestionKind.REGION, region.id(), region.name(),
                        region.code() == null ? null : region.code().toLowerCase(Locale.ROOT))));
        corridorService.getAll(null).stream()
                .filter(corridor -> corridor.status() == CorridorStatus.PUBLISHED && contains(corridor.name(), needle))
                .forEach(corridor -> found.add(new SuggestionResponse(SuggestionKind.CORRIDOR, corridor.id(), corridor.name(), corridor.slug())));
        stayService.getAll(null).stream()
                .filter(stay -> stay.status() == ListingStatus.PUBLISHED && contains(stay.name(), needle))
                .forEach(stay -> found.add(new SuggestionResponse(SuggestionKind.STAY, stay.id(), stay.name(), null)));
        foodService.getAll(null, null).stream()
                .filter(food -> food.status() == ListingStatus.PUBLISHED && contains(food.name(), needle))
                .forEach(food -> found.add(new SuggestionResponse(SuggestionKind.FOOD, food.id(), food.name(), food.slug())));
        String lower = needle.toLowerCase(Locale.ROOT);
        return found.stream()
                .sorted(Comparator.comparing((SuggestionResponse one) -> one.name().toLowerCase(Locale.ROOT).startsWith(lower) ? 0 : 1)
                        .thenComparing(SuggestionResponse::name, String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .toList();
    }

    @Override
    public List<SearchHitResponse> cards(SearchType type, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<UUID> wanted = new LinkedHashSet<>(ids);
        return switch (type == null ? SearchType.DESTINATION : type) {
            case DESTINATION -> destinationHits(inOrder(destinationService.findPublished(wanted), DestinationResponse::id, ids));
            case STAY -> stayHits(inOrder(stayService.getAll(null).stream()
                    .filter(stay -> stay.status() == ListingStatus.PUBLISHED && wanted.contains(stay.id())).toList(), StayResponse::id, ids));
            case FOOD -> foodHits(inOrder(foodService.getAll(null, null).stream()
                    .filter(food -> food.status() == ListingStatus.PUBLISHED && wanted.contains(food.id())).toList(), FoodResponse::id, ids));
            case CORRIDOR -> corridorHits(inOrder(corridorService.getAll(null).stream()
                    .filter(corridor -> corridor.status() == CorridorStatus.PUBLISHED && wanted.contains(corridor.id())).toList(), CorridorResponse::id, ids));
        };
    }

    // Destinations: the database query, or the ring around one of them.

    private DestinationSearch criteria(SearchRequest request) {
        DestinationSearch.Seal seal = null;
        if (request.sealOnly()) {
            TravelerSealBar bar = verificationService.travelerSeal();
            seal = new DestinationSearch.Seal(bar.minRatings(), bar.minAverage());
        }
        return new DestinationSearch(request.q(), request.regions(), request.provinces(), request.categoryIds(),
                request.tagIds(), request.interestIds(), seal);
    }

    /** Published attractions within the radius, nearest first, the centre itself left out; the chips then narrow them. */
    private List<DestinationResponse> destinationsNear(SearchRequest request) {
        DestinationSummary centre = destinationService.findSummary(request.near()).filter(DestinationSummary::published).orElse(null);
        if (centre == null || centre.latitude() == null || centre.longitude() == null) {
            return List.of();
        }
        Map<UUID, UUID> zones = zones();
        List<DestinationResponse> around = destinationService
                .findNearby(centre.latitude().doubleValue(), centre.longitude().doubleValue(), NEAR_RADIUS_KM, NEAR_LIMIT).stream()
                .filter(place -> !place.id().equals(centre.id()) && place.status() == ListingStatus.PUBLISHED)
                .filter(place -> matches(request.q(), place.name(), place.shortDescription(), place.address()))
                .filter(place -> inProvinces(request.provinces(), place.provinceId()))
                .filter(place -> inRegions(request.regions(), place.provinceId(), place.tourismRegionIds(), zones))
                .filter(place -> anyOf(request.categoryIds(), place.categoryIds()))
                .filter(place -> anyOf(request.tagIds(), place.tagIds()))
                .filter(place -> anyOf(request.interestIds(), place.interestIds()))
                .toList();
        if (!request.sealOnly() || around.isEmpty()) {
            return around;
        }
        Set<UUID> sealed = verificationService.findStatuses(around.stream().map(DestinationResponse::id).toList()).stream()
                .filter(status -> Boolean.TRUE.equals(status.travelerRecommended()))
                .map(VerificationResponse::destinationId)
                .collect(Collectors.toSet());
        return around.stream().filter(place -> sealed.contains(place.id())).toList();
    }

    // The lists of the few kinds, filtered here. Only destinations carry the Traveler seal.

    private List<StayResponse> stays(SearchRequest request) {
        if (request.sealOnly()) {
            return List.of();
        }
        Map<UUID, UUID> zones = zones();
        Stream<StayResponse> kept = (request.near() != null ? stayService.getNear(request.near()) : stayService.getAll(null)).stream()
                .filter(stay -> stay.status() == ListingStatus.PUBLISHED)
                .filter(stay -> matches(request.q(), stay.name(), stay.shortDescription(), stay.address()))
                .filter(stay -> inProvinces(request.provinces(), stay.provinceId()))
                .filter(stay -> inRegions(request.regions(), stay.provinceId(), List.of(), zones))
                .filter(stay -> anyOf(request.interestIds(), stay.interestIds()));
        return request.near() != null ? kept.toList()
                : kept.sorted(Comparator.comparing(StayResponse::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private List<FoodResponse> foods(SearchRequest request) {
        if (request.sealOnly()) {
            return List.of();
        }
        Map<UUID, UUID> zones = zones();
        Stream<FoodResponse> kept = (request.near() != null ? foodService.getNear(request.near()) : foodService.getAll(null, null)).stream()
                .filter(food -> food.status() == ListingStatus.PUBLISHED)
                .filter(food -> matches(request.q(), food.name(), food.shortDescription(), food.address()))
                .filter(food -> inProvinces(request.provinces(), food.provinceId()))
                .filter(food -> inRegions(request.regions(), food.provinceId(), List.of(), zones))
                .filter(food -> anyOf(request.categoryIds(), food.categoryIds()))
                .filter(food -> anyOf(request.interestIds(), food.interestIds()));
        return request.near() != null ? kept.toList()
                : kept.sorted(Comparator.comparing(FoodResponse::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    /** A corridor is in a province or a region through its stops; "near" a destination means stopping there. */
    private List<CorridorResponse> corridors(SearchRequest request) {
        if (request.sealOnly()) {
            return List.of();
        }
        return corridorService.getAll(null).stream()
                .filter(corridor -> corridor.status() == CorridorStatus.PUBLISHED)
                .filter(corridor -> matches(request.q(), corridor.name(), corridor.shortDescription()))
                .filter(corridor -> request.near() == null || corridor.stopDestinationIds().contains(request.near()))
                .filter(corridor -> anyOf(request.provinces(), corridor.stopProvinceIds()))
                .filter(corridor -> anyOf(request.regions(), corridor.stopRegionIds()))
                .filter(corridor -> anyOf(request.interestIds(), corridor.interestIds()))
                .sorted(Comparator.comparing(CorridorResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Each province's zone, for the region filter on the kinds that are not shelved in tourism regions. */
    private Map<UUID, UUID> zones() {
        Map<UUID, UUID> zones = new HashMap<>();
        for (ProvinceSummary province : provinceService.findSummaries()) {
            if (province.regionId() != null) {
                zones.put(province.id(), province.regionId());
            }
        }
        return zones;
    }

    // Cards, with ratings and standing looked up a page at a time.

    private List<SearchHitResponse> destinationHits(List<DestinationResponse> places) {
        if (places.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = places.stream().map(DestinationResponse::id).toList();
        Map<UUID, ReviewSummaryResponse> reviews = reviews(ReviewTargetType.DESTINATION, ids);
        Map<UUID, VerificationResponse> standing = new HashMap<>();
        for (int from = 0; from < ids.size(); from += SUMMARY_BATCH) {
            verificationService.findStatuses(ids.subList(from, Math.min(from + SUMMARY_BATCH, ids.size())))
                    .forEach(status -> standing.put(status.destinationId(), status));
        }
        Map<UUID, String> provinces = names.provinceNames();
        Map<UUID, String> categories = names.categoryNames();
        return places.stream()
                .map(place -> mapper.toHit(place, provinces, categories, reviews.get(place.id()), standing.get(place.id())))
                .toList();
    }

    private List<SearchHitResponse> stayHits(List<StayResponse> stays) {
        if (stays.isEmpty()) {
            return List.of();
        }
        Map<UUID, ReviewSummaryResponse> reviews = reviews(ReviewTargetType.STAY, stays.stream().map(StayResponse::id).toList());
        Map<UUID, String> provinces = names.provinceNames();
        return stays.stream().map(stay -> mapper.toHit(stay, provinces, reviews.get(stay.id()))).toList();
    }

    private List<SearchHitResponse> foodHits(List<FoodResponse> foods) {
        if (foods.isEmpty()) {
            return List.of();
        }
        Map<UUID, ReviewSummaryResponse> reviews = reviews(ReviewTargetType.FOOD, foods.stream().map(FoodResponse::id).toList());
        Map<UUID, String> provinces = names.provinceNames();
        Map<UUID, String> categories = names.categoryNames();
        return foods.stream().map(food -> mapper.toHit(food, provinces, categories, reviews.get(food.id()))).toList();
    }

    private List<SearchHitResponse> corridorHits(List<CorridorResponse> corridors) {
        if (corridors.isEmpty()) {
            return List.of();
        }
        Map<UUID, ReviewSummaryResponse> reviews = reviews(ReviewTargetType.CORRIDOR, corridors.stream().map(CorridorResponse::id).toList());
        return corridors.stream().map(corridor -> mapper.toHit(corridor, reviews.get(corridor.id()))).toList();
    }

    private Map<UUID, ReviewSummaryResponse> reviews(ReviewTargetType type, List<UUID> ids) {
        Map<UUID, ReviewSummaryResponse> byId = new HashMap<>();
        for (int from = 0; from < ids.size(); from += SUMMARY_BATCH) {
            reviewService.summaries(type, ids.subList(from, Math.min(from + SUMMARY_BATCH, ids.size())))
                    .forEach(summary -> byId.put(summary.targetId(), summary));
        }
        return byId;
    }

    // Helpers.

    private static <T> Page<SearchHitResponse> paged(List<T> all, PageRequest page, Function<List<T>, List<SearchHitResponse>> hits) {
        int from = (int) Math.min(page.getOffset(), all.size());
        int to = Math.min(from + page.getPageSize(), all.size());
        return new PageImpl<>(hits.apply(all.subList(from, to)), page, all.size());
    }

    /** Rows grouped by province (a corridor under each province it stops in), the first few of each made into cards. */
    private static <T> List<ProvinceGroupResponse> grouped(List<T> rows, Function<T, Collection<UUID>> provincesOf,
                                                           Function<List<T>, List<SearchHitResponse>> hits, int perProvince,
                                                           Map<UUID, String> provinceNames) {
        Map<UUID, List<T>> byProvince = new HashMap<>();
        for (T row : rows) {
            for (UUID provinceId : provincesOf.apply(row)) {
                if (provinceId != null) {
                    byProvince.computeIfAbsent(provinceId, id -> new ArrayList<>()).add(row);
                }
            }
        }
        return byProvince.entrySet().stream()
                .map(group -> new ProvinceGroupResponse(group.getKey(), provinceNames.get(group.getKey()), group.getValue().size(),
                        hits.apply(group.getValue().subList(0, Math.min(perProvince, group.getValue().size())))))
                .sorted(Comparator.comparing(ProvinceGroupResponse::provinceName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private static List<UUID> one(UUID id) {
        return id == null ? List.of() : List.of(id);
    }

    private static <T> List<T> inOrder(List<T> rows, Function<T, UUID> id, List<UUID> order) {
        Map<UUID, Integer> rank = new HashMap<>();
        for (int index = 0; index < order.size(); index++) {
            rank.putIfAbsent(order.get(index), index);
        }
        return rows.stream().sorted(Comparator.comparing(row -> rank.getOrDefault(id.apply(row), Integer.MAX_VALUE))).toList();
    }

    private static boolean matches(String q, String... texts) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String needle = q.trim().toLowerCase(Locale.ROOT);
        for (String text : texts) {
            if (text != null && text.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(String name, String needle) {
        return name != null && name.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private static boolean inProvinces(Set<UUID> wanted, UUID provinceId) {
        return wanted.isEmpty() || (provinceId != null && wanted.contains(provinceId));
    }

    /** A zone through the province, or any tourism region the place is shelved in. */
    private static boolean inRegions(Set<UUID> wanted, UUID provinceId, Collection<UUID> tourismRegionIds, Map<UUID, UUID> zones) {
        if (wanted.isEmpty()) {
            return true;
        }
        UUID zone = provinceId == null ? null : zones.get(provinceId);
        return (zone != null && wanted.contains(zone)) || (tourismRegionIds != null && tourismRegionIds.stream().anyMatch(wanted::contains));
    }

    private static boolean anyOf(Set<UUID> wanted, Collection<UUID> has) {
        return wanted == null || wanted.isEmpty() || (has != null && has.stream().anyMatch(wanted::contains));
    }
}
