package ams.asmtravel.api.services.activity.popular.service.impl;

import ams.asmtravel.api.services.activity.popular.config.PopularProperties;
import ams.asmtravel.api.services.activity.popular.domain.BrowseEvent;
import ams.asmtravel.api.services.activity.popular.domain.BrowseKinds;
import ams.asmtravel.api.services.activity.popular.domain.BrowseTarget;
import ams.asmtravel.api.services.activity.popular.domain.BrowseTotal;
import ams.asmtravel.api.services.activity.popular.domain.PopularItem;
import ams.asmtravel.api.services.activity.popular.domain.PopularReason;
import ams.asmtravel.api.services.activity.popular.dto.request.BrowseEventRequest;
import ams.asmtravel.api.services.activity.popular.dto.response.PopularResponse;
import ams.asmtravel.api.services.activity.popular.mapper.PopularMapper;
import ams.asmtravel.api.services.activity.popular.repository.BrowseEventRepository;
import ams.asmtravel.api.services.activity.popular.service.PopularService;
import ams.asmtravel.api.services.corridor.domain.CorridorStatus;
import ams.asmtravel.api.services.corridor.service.CorridorService;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.destination.activity.service.ActivityService;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.location.province.domain.ProvinceSummary;
import ams.asmtravel.api.services.location.province.service.ProvinceService;
import ams.asmtravel.api.services.location.region.exception.RegionNotFoundException;
import ams.asmtravel.api.services.location.region.service.RegionService;
import ams.asmtravel.api.services.review.review.domain.ReviewTargetType;
import ams.asmtravel.api.services.review.review.dto.response.ReviewSummaryResponse;
import ams.asmtravel.api.services.review.review.service.ReviewService;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import ams.asmtravel.api.services.verification.service.VerificationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Browses and Popular, from published places only: the most browsed first, and when fewer places than
 * asked have been browsed, the row filled in a stable order so a fresh server never shows it empty.
 */
@Service
public class PopularServiceImpl implements PopularService {

    private static final int SUMMARY_BATCH = 100;

    private final BrowseEventRepository eventRepository;
    private final DestinationService destinationService;
    private final StayService stayService;
    private final FoodService foodService;
    private final ActivityService activityService;
    private final CorridorService corridorService;
    private final RegionService regionService;
    private final ProvinceService provinceService;
    private final ObjectProvider<ReviewService> reviewService;
    private final ObjectProvider<VerificationService> verificationService;
    private final PopularMapper mapper;
    private final PopularProperties properties;

    public PopularServiceImpl(BrowseEventRepository eventRepository, DestinationService destinationService, StayService stayService,
                              FoodService foodService, ActivityService activityService, CorridorService corridorService, RegionService regionService,
                              ProvinceService provinceService, ObjectProvider<ReviewService> reviewService,
                              ObjectProvider<VerificationService> verificationService, PopularMapper mapper, PopularProperties properties) {
        this.eventRepository = eventRepository;
        this.destinationService = destinationService;
        this.stayService = stayService;
        this.foodService = foodService;
        this.activityService = activityService;
        this.corridorService = corridorService;
        this.regionService = regionService;
        this.provinceService = provinceService;
        this.reviewService = reviewService;
        this.verificationService = verificationService;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void record(BrowseEventRequest request, UUID userId) {
        BrowseTarget type = request.resolvedType();
        UUID target = request.resolvedTarget();
        boolean firstToday = target != null && !seenToday(type, target, userId, request.sessionId());
        eventRepository.save(mapper.toEvent(request, userId));
        if (firstToday && (userId != null || request.sessionId() != null)) {
            recordView(type, target);
        }
    }

    /** A page's first open of the day counts as a view on the place itself; a corridor keeps no count. */
    private void recordView(BrowseTarget type, UUID target) {
        if (type == null) {
            return;
        }
        switch (type) {
            case DESTINATION -> {
                if (destinationService.exists(target)) {
                    destinationService.recordView(target);
                }
            }
            case STAY -> {
                if (stayService.exists(target)) {
                    stayService.recordView(target);
                }
            }
            case FOOD -> {
                if (foodService.exists(target)) {
                    foodService.recordView(target);
                }
            }
            case ACTIVITY -> {
                if (activityService.exists(target)) {
                    activityService.recordView(target);
                }
            }
            case CORRIDOR -> {
            }
        }
    }

    @Override
    @Transactional
    public boolean recordBrowse(UUID userId, BrowseTarget type, UUID targetId) {
        boolean firstToday = !seenToday(type, targetId, userId, null);
        BrowseEvent event = new BrowseEvent();
        event.setUserId(userId);
        event.setTargetType(type);
        event.setTargetId(targetId);
        event.setEventType(BrowseKinds.EXPLORATION);
        eventRepository.save(event);
        if (firstToday) {
            recordView(type, targetId);
        }
        return firstToday;
    }

    @Override
    @Transactional(readOnly = true)
    public PopularResponse popular(BrowseTarget type, Set<UUID> regionIds, Set<UUID> provinceIds, Set<UUID> interestIds, Integer limit) {
        Set<UUID> regions = regionIds == null ? Set.of() : regionIds;
        for (UUID regionId : regions) {
            if (!regionService.exists(regionId)) {
                throw new RegionNotFoundException();
            }
        }
        BrowseTarget kind = type == null ? BrowseTarget.DESTINATION : type;
        int size = limit == null ? properties.size() : limit;

        // Published places of the kind, only those the filters keep, A to Z.
        Map<UUID, Candidate> catalogue = new LinkedHashMap<>();
        catalogue(kind, regions, provinceIds == null ? Set.of() : provinceIds, interestIds == null ? Set.of() : interestIds)
                .forEach(candidate -> catalogue.put(candidate.id(), candidate));

        // Whatever visitors browse most.
        List<PopularItem> items = new ArrayList<>();
        Set<UUID> taken = new HashSet<>();
        for (BrowseTotal total : recentTotals(kind)) {
            if (items.size() >= size) {
                break;
            }
            if (total.browses() > 0 && catalogue.containsKey(total.targetId()) && taken.add(total.targetId())) {
                items.add(new PopularItem(kind, total.targetId(), total.browses(), PopularReason.MOST_BROWSED));
            }
        }
        if (items.size() < size) {
            fill(items, taken, kind, catalogue.values(), size);
        }
        return mapper.toResponse(properties.days(), regions.size() == 1 ? regions.iterator().next() : null, kind, items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrowseTotal> recentTotals() {
        return recentTotals(BrowseTarget.DESTINATION);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrowseTotal> recentTotals(BrowseTarget type) {
        LocalDate first = LocalDate.now(properties.zone()).minusDays(properties.days() - 1L);
        return eventRepository.totalsSince(type, first.atStartOfDay(properties.zone()).toInstant());
    }

    @Override
    public int windowDays() {
        return properties.days();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrowseTotal> totalsBetween(Instant from, Instant to) {
        return totalsBetween(BrowseTarget.DESTINATION, from, to);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrowseTotal> totalsBetween(BrowseTarget type, Instant from, Instant to) {
        return eventRepository.totalsBetween(type, from, to);
    }

    @Override
    public ZoneId zone() {
        return properties.zone();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> browsedDestinationIds(UUID userId) {
        return userId == null ? Set.of() : Set.copyOf(eventRepository.findBrowsedTargetIds(userId, BrowseTarget.DESTINATION));
    }

    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        eventRepository.forgetUser(event.userId());
    }

    /** Distinct (place, Cambodian day) pairs: opening Angkor Wat twice on Monday is one browse. */
    @Override
    @Transactional(readOnly = true)
    public long countDailyBrowses(UUID userId, Instant from, Instant to) {
        Set<String> daily = new HashSet<>();
        // Attractions, stays, food places and activities alike are destinations; a corridor is not.
        List<BrowseTarget> kinds = List.of(BrowseTarget.DESTINATION, BrowseTarget.STAY, BrowseTarget.FOOD, BrowseTarget.ACTIVITY);
        for (BrowseEvent event : eventRepository.findByUserBetween(userId, kinds, from, to)) {
            daily.add(event.getTargetId() + "@" + event.getCreatedAt().atZone(properties.zone()).toLocalDate());
        }
        return daily.size();
    }

    /** Whether this visitor — or, anonymous, this session — opened the place earlier today in Cambodia. */
    private boolean seenToday(BrowseTarget type, UUID targetId, UUID userId, String sessionId) {
        ZonedDateTime start = LocalDate.now(properties.zone()).atStartOfDay(properties.zone());
        Instant from = start.toInstant();
        Instant to = start.plusDays(1).toInstant();
        if (userId != null) {
            return eventRepository.existsByTargetTypeAndTargetIdAndUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    type, targetId, userId, from, to);
        }
        return sessionId != null
                && eventRepository.existsByTargetTypeAndTargetIdAndSessionIdAndUserIdIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        type, targetId, sessionId, from, to);
    }

    // The catalogue of one kind, and the filler.

    /** A published place the filters keep, with what the filler sorts on. */
    private record Candidate(UUID id, String name, boolean verified, BigDecimal averageRating, long ratingCount) {
        Candidate rated(ReviewSummaryResponse summary) {
            return summary == null ? this : new Candidate(id, name, verified, summary.averageRating(), summary.ratingCount());
        }

        Candidate markVerified(boolean yes) {
            return new Candidate(id, name, yes, averageRating, ratingCount);
        }
    }

    private List<Candidate> catalogue(BrowseTarget kind, Set<UUID> regionIds, Set<UUID> provinceIds, Set<UUID> interestIds) {
        return switch (kind) {
            case DESTINATION -> destinationService.findPublishedSummaries().stream()
                    .filter(place -> regionIds.isEmpty() || regionIds.stream().anyMatch(place::inRegion))
                    .filter(place -> provinceIds.isEmpty() || provinceIds.contains(place.provinceId()))
                    .filter(place -> anyOf(interestIds, place.interestIds()))
                    .map(place -> new Candidate(place.id(), place.name(), false, null, 0))
                    .toList();
            case STAY -> {
                Map<UUID, UUID> zones = zones();
                yield stayService.getAll(null).stream()
                        .filter(stay -> stay.status() == ListingStatus.PUBLISHED)
                        .filter(stay -> inRegions(regionIds, stay.provinceId(), zones))
                        .filter(stay -> provinceIds.isEmpty() || provinceIds.contains(stay.provinceId()))
                        .filter(stay -> anyOf(interestIds, stay.interestIds()))
                        .sorted(Comparator.comparing(stay -> stay.name(), String.CASE_INSENSITIVE_ORDER))
                        .map(stay -> new Candidate(stay.id(), stay.name(), false, null, 0))
                        .toList();
            }
            case FOOD -> {
                Map<UUID, UUID> zones = zones();
                yield foodService.getAll(null, null).stream()
                        .filter(food -> food.status() == ListingStatus.PUBLISHED)
                        .filter(food -> inRegions(regionIds, food.provinceId(), zones))
                        .filter(food -> provinceIds.isEmpty() || provinceIds.contains(food.provinceId()))
                        .filter(food -> anyOf(interestIds, food.interestIds()))
                        .sorted(Comparator.comparing(food -> food.name(), String.CASE_INSENSITIVE_ORDER))
                        .map(food -> new Candidate(food.id(), food.name(), false, null, 0))
                        .toList();
            }
            case ACTIVITY -> {
                Map<UUID, UUID> zones = zones();
                yield activityService.getAll(null, null).stream()
                        .filter(activity -> activity.status() == ListingStatus.PUBLISHED)
                        /* Shelved in tourism regions like an attraction, in a zone through its province like a stay. */
                        .filter(activity -> inRegions(regionIds, activity.provinceId(), zones) || anyOf(regionIds, activity.tourismRegionIds()))
                        .filter(activity -> provinceIds.isEmpty() || provinceIds.contains(activity.provinceId()))
                        .filter(activity -> anyOf(interestIds, activity.interestIds()))
                        .sorted(Comparator.comparing(activity -> activity.name(), String.CASE_INSENSITIVE_ORDER))
                        .map(activity -> new Candidate(activity.id(), activity.name(), false, null, 0))
                        .toList();
            }
            case CORRIDOR -> corridorService.getAll(null).stream()
                    .filter(corridor -> corridor.status() == CorridorStatus.PUBLISHED)
                    .filter(corridor -> anyOf(regionIds, corridor.stopRegionIds()))
                    .filter(corridor -> anyOf(provinceIds, corridor.stopProvinceIds()))
                    .filter(corridor -> anyOf(interestIds, corridor.interestIds()))
                    .sorted(Comparator.comparing(corridor -> corridor.name(), String.CASE_INSENSITIVE_ORDER))
                    .map(corridor -> new Candidate(corridor.id(), corridor.name(), false, null, 0))
                    .toList();
        };
    }

    /** Verified places first (a corridor is never verified), then the best rated, then the rest A to Z. */
    private void fill(List<PopularItem> items, Set<UUID> taken, BrowseTarget kind, Collection<Candidate> catalogue, int size) {
        List<Candidate> left = catalogue.stream().filter(candidate -> !taken.contains(candidate.id())).toList();
        if (left.isEmpty()) {
            return;
        }
        Map<UUID, ReviewSummaryResponse> ratings = ratings(kind, left.stream().map(Candidate::id).toList());
        Set<UUID> verified = kind == BrowseTarget.CORRIDOR ? Set.of() : verified(left.stream().map(Candidate::id).toList());
        List<Candidate> ranked = left.stream()
                .map(candidate -> candidate.rated(ratings.get(candidate.id())).markVerified(verified.contains(candidate.id())))
                .sorted(Comparator.comparing(Candidate::verified, Comparator.reverseOrder())
                        .thenComparing(Candidate::averageRating, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Candidate::ratingCount, Comparator.reverseOrder())
                        .thenComparing(Candidate::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        for (Candidate candidate : ranked) {
            if (items.size() >= size) {
                return;
            }
            PopularReason reason = candidate.verified() ? PopularReason.VERIFIED
                    : candidate.ratingCount() > 0 ? PopularReason.TOP_RATED : PopularReason.CATALOGUE;
            items.add(new PopularItem(kind, candidate.id(), 0, reason));
            taken.add(candidate.id());
        }
    }

    private Map<UUID, ReviewSummaryResponse> ratings(BrowseTarget kind, List<UUID> ids) {
        ReviewTargetType type = ReviewTargetType.valueOf(kind.name());
        Map<UUID, ReviewSummaryResponse> byId = new HashMap<>();
        for (int from = 0; from < ids.size(); from += SUMMARY_BATCH) {
            reviewService.getObject().summaries(type, ids.subList(from, Math.min(from + SUMMARY_BATCH, ids.size())))
                    .forEach(summary -> byId.put(summary.targetId(), summary));
        }
        return byId;
    }

    private Set<UUID> verified(List<UUID> ids) {
        Set<UUID> verified = new HashSet<>();
        for (int from = 0; from < ids.size(); from += SUMMARY_BATCH) {
            verificationService.getObject().findStatuses(ids.subList(from, Math.min(from + SUMMARY_BATCH, ids.size()))).stream()
                    .filter(status -> status.verified())
                    .forEach(status -> verified.add(status.destinationId()));
        }
        return verified;
    }

    /** Each province's zone, for the region filter on stays and food. */
    private Map<UUID, UUID> zones() {
        Map<UUID, UUID> zones = new HashMap<>();
        for (ProvinceSummary province : provinceService.findSummaries()) {
            if (province.regionId() != null) {
                zones.put(province.id(), province.regionId());
            }
        }
        return zones;
    }

    private static boolean inRegions(Set<UUID> wanted, UUID provinceId, Map<UUID, UUID> zones) {
        if (wanted.isEmpty()) {
            return true;
        }
        UUID zone = provinceId == null ? null : zones.get(provinceId);
        return zone != null && wanted.contains(zone);
    }

    private static boolean anyOf(Set<UUID> wanted, Collection<UUID> has) {
        return wanted.isEmpty() || (has != null && has.stream().anyMatch(wanted::contains));
    }
}
