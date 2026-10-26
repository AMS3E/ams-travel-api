package ams.asmtravel.api.services.search.service.impl;

import ams.asmtravel.api.services.corridor.domain.CorridorStatus;
import ams.asmtravel.api.services.corridor.dto.response.CorridorResponse;
import ams.asmtravel.api.services.corridor.service.CorridorService;
import ams.asmtravel.api.services.destination.activity.dto.response.ActivityResponse;
import ams.asmtravel.api.services.destination.activity.service.ActivityService;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSearch;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.domain.KhmerText;
import ams.asmtravel.api.services.destination.destination.domain.NearbyDestination;
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
import ams.asmtravel.api.services.search.domain.KhmerNames;
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
 * Destinations are searched in the database, a page at a time. Stays, food places, activities and corridors are few,
 * so their published lists are filtered here; the same cards come back either way. Around a destination
 * ({@code near}) the order is nearest first instead of A to Z.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private static final int MAX_PAGE = 100;
    private static final int SUMMARY_BATCH = 100;
    /** How much of the Khmer description a Khmer card shows. */
    private static final int SUMMARY_LENGTH = 300;
    /** Attractions around a destination: how far out unless asked ({@code radiusKm}), and at most how many. */
    private static final double NEAR_RADIUS_KM = 25;
    private static final int NEAR_LIMIT = 100;

    private final DestinationService destinationService;
    private final StayService stayService;
    private final FoodService foodService;
    private final ActivityService activityService;
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
        boolean khmer = request.khmer();
        return switch (request.typeOrDefault()) {
            case DESTINATION -> {
                if (request.near() != null) {
                    yield paged(destinationsNear(request), page, rows -> nearHits(rows, khmer));
                }
                Page<DestinationResponse> found = destinationService.searchPublished(criteria(request),
                        page.withSort(Sort.by("name").ascending()));
                yield new PageImpl<>(destinationHits(found.getContent(), khmer), page, found.getTotalElements());
            }
            case STAY -> paged(stays(request), page, rows -> stayHits(rows, khmer));
            case FOOD -> paged(foods(request), page, rows -> foodHits(rows, khmer));
            case ACTIVITY -> paged(activities(request), page, rows -> activityHits(rows, khmer));
            case CORRIDOR -> paged(corridors(request), page, rows -> corridorHits(rows, khmer));
        };
    }

    @Override
    public List<ProvinceGroupResponse> byProvince(SearchRequest request, int perProvince) {
        Map<UUID, String> provinceNames = names.provinceNames();
        boolean khmer = request.khmer();
        Map<UUID, String> provinceNamesKm = khmer ? names.khmerNames().provincesKm() : null;
        return switch (request.typeOrDefault()) {
            case DESTINATION -> request.near() != null
                    ? grouped(destinationsNear(request), near -> one(near.place().provinceId()), rows -> nearHits(rows, khmer),
                            perProvince, provinceNames, provinceNamesKm)
                    : grouped(destinationService.searchPublished(criteria(request), Pageable.unpaged(Sort.by("name").ascending())).getContent(),
                            place -> one(place.provinceId()), rows -> destinationHits(rows, khmer), perProvince, provinceNames, provinceNamesKm);
            case STAY -> grouped(stays(request), stay -> one(stay.provinceId()), rows -> stayHits(rows, khmer), perProvince,
                    provinceNames, provinceNamesKm);
            case FOOD -> grouped(foods(request), food -> one(food.provinceId()), rows -> foodHits(rows, khmer), perProvince,
                    provinceNames, provinceNamesKm);
            case ACTIVITY -> grouped(activities(request), activity -> one(activity.provinceId()), rows -> activityHits(rows, khmer),
                    perProvince, provinceNames, provinceNamesKm);
            case CORRIDOR -> grouped(corridors(request), CorridorResponse::stopProvinceIds, rows -> corridorHits(rows, khmer), perProvince,
                    provinceNames, provinceNamesKm);
        };
    }

    @Override
    public List<SuggestionResponse> suggest(String text, int limit, boolean khmer) {
        String needle = text == null ? "" : text.trim();
        if (needle.isEmpty() || limit < 1) {
            return List.of();
        }
        List<SuggestionResponse> found = new ArrayList<>();
        destinationService.suggestPublished(needle, limit)
                .forEach(place -> found.add(suggestion(SuggestionKind.DESTINATION, place.id(), place.name(), place.nameKm(), place.slug(), khmer)));
        provinceService.findSummaries().stream().filter(province -> contains(province.name(), needle) || contains(province.nameKm(), needle))
                .forEach(province -> found.add(suggestion(SuggestionKind.PROVINCE, province.id(), province.name(), province.nameKm(),
                        province.slug(), khmer)));
        regionService.getAll(RegionKind.TOURISM).stream()
                .filter(region -> !Boolean.FALSE.equals(region.isActive()) && (contains(region.name(), needle) || contains(region.nameKm(), needle)))
                .forEach(region -> found.add(suggestion(SuggestionKind.REGION, region.id(), region.name(), region.nameKm(),
                        region.code() == null ? null : region.code().toLowerCase(Locale.ROOT), khmer)));
        corridorService.getAll(null).stream()
                .filter(corridor -> corridor.status() == CorridorStatus.PUBLISHED
                        && (contains(corridor.name(), needle) || contains(corridor.nameKm(), needle)))
                .forEach(corridor -> found.add(suggestion(SuggestionKind.CORRIDOR, corridor.id(), corridor.name(), corridor.nameKm(),
                        corridor.slug(), khmer)));
        stayService.getAll(null).stream()
                .filter(stay -> stay.status() == ListingStatus.PUBLISHED && (contains(stay.name(), needle) || contains(stay.nameKm(), needle)))
                .forEach(stay -> found.add(suggestion(SuggestionKind.STAY, stay.id(), stay.name(), stay.nameKm(), null, khmer)));
        foodService.getAll(null, null).stream()
                .filter(food -> food.status() == ListingStatus.PUBLISHED && (contains(food.name(), needle) || contains(food.nameKm(), needle)))
                .forEach(food -> found.add(suggestion(SuggestionKind.FOOD, food.id(), food.name(), food.nameKm(), food.slug(), khmer)));
        activityService.getAll(null, null).stream()
                .filter(activity -> activity.status() == ListingStatus.PUBLISHED
                        && (contains(activity.name(), needle) || contains(activity.nameKm(), needle)))
                .forEach(activity -> found.add(suggestion(SuggestionKind.ACTIVITY, activity.id(), activity.name(), activity.nameKm(),
                        activity.slug(), khmer)));
        String lower = needle.toLowerCase(Locale.ROOT);
        return found.stream()
                .sorted(Comparator.comparing((SuggestionResponse one) -> one.name().toLowerCase(Locale.ROOT).startsWith(lower) ? 0 : 1)
                        .thenComparing(SuggestionResponse::name, String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .toList();
    }

    @Override
    public List<SearchHitResponse> cards(SearchType type, List<UUID> ids, boolean khmer) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<UUID> wanted = new LinkedHashSet<>(ids);
        return switch (type == null ? SearchType.DESTINATION : type) {
            case DESTINATION -> destinationHits(inOrder(destinationService.findPublished(wanted), DestinationResponse::id, ids), khmer);
            case STAY -> stayHits(inOrder(stayService.getAll(null).stream()
                    .filter(stay -> stay.status() == ListingStatus.PUBLISHED && wanted.contains(stay.id())).toList(), StayResponse::id, ids), khmer);
            case FOOD -> foodHits(inOrder(foodService.getAll(null, null).stream()
                    .filter(food -> food.status() == ListingStatus.PUBLISHED && wanted.contains(food.id())).toList(), FoodResponse::id, ids), khmer);
            case ACTIVITY -> activityHits(inOrder(activityService.getAll(null, null).stream()
                    .filter(activity -> activity.status() == ListingStatus.PUBLISHED && wanted.contains(activity.id())).toList(),
                    ActivityResponse::id, ids), khmer);
            case CORRIDOR -> corridorHits(inOrder(corridorService.getAll(null).stream()
                    .filter(corridor -> corridor.status() == CorridorStatus.PUBLISHED && wanted.contains(corridor.id())).toList(), CorridorResponse::id, ids),
                    khmer);
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

    /** Published attractions within the radius, nearest first with their distances, the centre itself left out; the chips then narrow them. */
    private List<NearbyDestination> destinationsNear(SearchRequest request) {
        DestinationSummary centre = destinationService.findSummary(request.near()).filter(DestinationSummary::published).orElse(null);
        if (centre == null || centre.latitude() == null || centre.longitude() == null) {
            return List.of();
        }
        Map<UUID, UUID> zones = zones();
        List<NearbyDestination> around = destinationService
                .findNearbyWithDistance(centre.latitude().doubleValue(), centre.longitude().doubleValue(),
                        request.radiusKmOr(NEAR_RADIUS_KM), NEAR_LIMIT).stream()
                .filter(near -> !near.place().id().equals(centre.id()) && near.place().status() == ListingStatus.PUBLISHED)
                .filter(near -> matches(request.q(), near.place().name(), near.place().shortDescription(), near.place().address(),
                        near.place().nameKm(), near.place().addressKm()))
                .filter(near -> inProvinces(request.provinces(), near.place().provinceId()))
                .filter(near -> inRegions(request.regions(), near.place().provinceId(), near.place().tourismRegionIds(), zones))
                .filter(near -> anyOf(request.categoryIds(), near.place().categoryIds()))
                .filter(near -> anyOf(request.tagIds(), near.place().tagIds()))
                .filter(near -> anyOf(request.interestIds(), near.place().interestIds()))
                .toList();
        if (!request.sealOnly() || around.isEmpty()) {
            return around;
        }
        Set<UUID> sealed = verificationService.findStatuses(around.stream().map(near -> near.place().id()).toList()).stream()
                .filter(status -> Boolean.TRUE.equals(status.travelerRecommended()))
                .map(VerificationResponse::destinationId)
                .collect(Collectors.toSet());
        return around.stream().filter(near -> sealed.contains(near.place().id())).toList();
    }

    // The lists of the few kinds, filtered here; the Traveler seal is read for every kind.

    private List<StayResponse> stays(SearchRequest request) {
        Map<UUID, UUID> zones = zones();
        Stream<StayResponse> kept = (request.near() != null ? stayService.getNear(request.near()) : stayService.getAll(null)).stream()
                .filter(stay -> stay.status() == ListingStatus.PUBLISHED)
                .filter(stay -> matches(request.q(), stay.name(), stay.shortDescription(), stay.address(), stay.nameKm(), stay.addressKm()))
                .filter(stay -> inProvinces(request.provinces(), stay.provinceId()))
                .filter(stay -> inRegions(request.regions(), stay.provinceId(), List.of(), zones))
                .filter(stay -> anyOf(request.interestIds(), stay.interestIds()));
        List<StayResponse> stays = request.near() != null ? kept.toList()
                : kept.sorted(Comparator.comparing(StayResponse::name, String.CASE_INSENSITIVE_ORDER)).toList();
        return request.sealOnly() ? sealed(stays, StayResponse::id) : stays;
    }

    private List<FoodResponse> foods(SearchRequest request) {
        Map<UUID, UUID> zones = zones();
        Stream<FoodResponse> kept = (request.near() != null ? foodService.getNear(request.near()) : foodService.getAll(null, null)).stream()
                .filter(food -> food.status() == ListingStatus.PUBLISHED)
                .filter(food -> matches(request.q(), food.name(), food.shortDescription(), food.address(), food.nameKm(), food.addressKm()))
                .filter(food -> inProvinces(request.provinces(), food.provinceId()))
                .filter(food -> inRegions(request.regions(), food.provinceId(), List.of(), zones))
                .filter(food -> anyOf(request.categoryIds(), food.categoryIds()))
                .filter(food -> anyOf(request.interestIds(), food.interestIds()));
        List<FoodResponse> foods = request.near() != null ? kept.toList()
                : kept.sorted(Comparator.comparing(FoodResponse::name, String.CASE_INSENSITIVE_ORDER)).toList();
        return request.sealOnly() ? sealed(foods, FoodResponse::id) : foods;
    }

    /** An activity is classed by its interests alone, like a stay: a region filter reaches it through its province's zone. */
    private List<ActivityResponse> activities(SearchRequest request) {
        Map<UUID, UUID> zones = zones();
        Stream<ActivityResponse> kept = (request.near() != null ? activityService.getNear(request.near())
                : activityService.getAll(null, null)).stream()
                .filter(activity -> activity.status() == ListingStatus.PUBLISHED)
                .filter(activity -> matches(request.q(), activity.name(), activity.shortDescription(), activity.address(),
                        activity.nameKm(), activity.addressKm()))
                .filter(activity -> inProvinces(request.provinces(), activity.provinceId()))
                .filter(activity -> inRegions(request.regions(), activity.provinceId(), List.of(), zones))
                .filter(activity -> anyOf(request.interestIds(), activity.interestIds()));
        List<ActivityResponse> activities = request.near() != null ? kept.toList()
                : kept.sorted(Comparator.comparing(ActivityResponse::name, String.CASE_INSENSITIVE_ORDER)).toList();
        return request.sealOnly() ? sealed(activities, ActivityResponse::id) : activities;
    }

    /** The rows whose reviews have earned them the Traveler seal. */
    private <T> List<T> sealed(List<T> rows, Function<T, UUID> idOf) {
        if (rows.isEmpty()) {
            return rows;
        }
        Set<UUID> sealed = standings(rows.stream().map(idOf).toList()).values().stream()
                .filter(status -> Boolean.TRUE.equals(status.travelerRecommended()))
                .map(VerificationResponse::destinationId)
                .collect(Collectors.toSet());
        return rows.stream().filter(row -> sealed.contains(idOf.apply(row))).toList();
    }

    /** Verified, on what, and the Traveler seal, per place of any kind; the verification service answers 100 at a time. */
    private Map<UUID, VerificationResponse> standings(List<UUID> ids) {
        Map<UUID, VerificationResponse> standing = new HashMap<>();
        for (int from = 0; from < ids.size(); from += SUMMARY_BATCH) {
            verificationService.findStatuses(ids.subList(from, Math.min(from + SUMMARY_BATCH, ids.size())))
                    .forEach(status -> standing.put(status.destinationId(), status));
        }
        return standing;
    }

    /** A corridor is in a province or a region through its stops; "near" a destination means stopping there. */
    private List<CorridorResponse> corridors(SearchRequest request) {
        if (request.sealOnly()) {
            return List.of();
        }
        return corridorService.getAll(null).stream()
                .filter(corridor -> corridor.status() == CorridorStatus.PUBLISHED)
                .filter(corridor -> matches(request.q(), corridor.name(), corridor.shortDescription(), corridor.nameKm(),
                        corridor.shortDescriptionKm()))
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

    /** Cards for places found around a destination, each carrying how far it is, rounded to the metre. */
    private List<SearchHitResponse> nearHits(List<NearbyDestination> rows, boolean khmer) {
        Map<UUID, Double> metres = new HashMap<>();
        rows.forEach(near -> metres.put(near.place().id(), near.metres()));
        return destinationHits(rows.stream().map(NearbyDestination::place).toList(), khmer).stream()
                .map(hit -> hit.withDistanceMeters(metres.containsKey(hit.id()) ? (int) Math.round(metres.get(hit.id())) : null))
                .toList();
    }

    private List<SearchHitResponse> destinationHits(List<DestinationResponse> places, boolean khmer) {
        if (places.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = places.stream().map(DestinationResponse::id).toList();
        Map<UUID, ReviewSummaryResponse> reviews = reviews(ReviewTargetType.DESTINATION, ids);
        Map<UUID, VerificationResponse> standing = standings(ids);
        Map<UUID, String> provinces = names.provinceNames();
        Map<UUID, String> categories = names.categoryNames();
        KhmerNames words = khmer ? names.khmerNames() : null;
        return places.stream()
                .map(place -> {
                    SearchHitResponse hit = mapper.toHit(place, provinces, categories, reviews.get(place.id()), standing.get(place.id()));
                    return khmer ? mapper.inKhmer(hit, place.nameKm(), KhmerText.excerpt(place.descriptionKm(), SUMMARY_LENGTH), words) : hit;
                })
                .toList();
    }

    private List<SearchHitResponse> stayHits(List<StayResponse> stays, boolean khmer) {
        if (stays.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = stays.stream().map(StayResponse::id).toList();
        Map<UUID, ReviewSummaryResponse> reviews = reviews(ReviewTargetType.STAY, ids);
        Map<UUID, VerificationResponse> standing = standings(ids);
        Map<UUID, String> provinces = names.provinceNames();
        KhmerNames words = khmer ? names.khmerNames() : null;
        return stays.stream()
                .map(stay -> {
                    SearchHitResponse hit = mapper.toHit(stay, provinces, reviews.get(stay.id()), standing.get(stay.id()));
                    return khmer ? mapper.inKhmer(hit, stay.nameKm(), KhmerText.excerpt(stay.descriptionKm(), SUMMARY_LENGTH), words) : hit;
                })
                .toList();
    }

    private List<SearchHitResponse> foodHits(List<FoodResponse> foods, boolean khmer) {
        if (foods.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = foods.stream().map(FoodResponse::id).toList();
        Map<UUID, ReviewSummaryResponse> reviews = reviews(ReviewTargetType.FOOD, ids);
        Map<UUID, VerificationResponse> standing = standings(ids);
        Map<UUID, String> provinces = names.provinceNames();
        Map<UUID, String> categories = names.categoryNames();
        KhmerNames words = khmer ? names.khmerNames() : null;
        return foods.stream()
                .map(food -> {
                    SearchHitResponse hit = mapper.toHit(food, provinces, categories, reviews.get(food.id()), standing.get(food.id()));
                    return khmer ? mapper.inKhmer(hit, food.nameKm(), KhmerText.excerpt(food.descriptionKm(), SUMMARY_LENGTH), words) : hit;
                })
                .toList();
    }

    private List<SearchHitResponse> activityHits(List<ActivityResponse> activities, boolean khmer) {
        if (activities.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = activities.stream().map(ActivityResponse::id).toList();
        Map<UUID, ReviewSummaryResponse> reviews = reviews(ReviewTargetType.ACTIVITY, ids);
        Map<UUID, VerificationResponse> standing = standings(ids);
        Map<UUID, String> provinces = names.provinceNames();
        KhmerNames words = khmer ? names.khmerNames() : null;
        return activities.stream()
                .map(activity -> {
                    SearchHitResponse hit = mapper.toHit(activity, provinces, reviews.get(activity.id()), standing.get(activity.id()));
                    return khmer ? mapper.inKhmer(hit, activity.nameKm(), KhmerText.excerpt(activity.descriptionKm(), SUMMARY_LENGTH), words) : hit;
                })
                .toList();
    }

    private List<SearchHitResponse> corridorHits(List<CorridorResponse> corridors, boolean khmer) {
        if (corridors.isEmpty()) {
            return List.of();
        }
        Map<UUID, ReviewSummaryResponse> reviews = reviews(ReviewTargetType.CORRIDOR, corridors.stream().map(CorridorResponse::id).toList());
        KhmerNames words = khmer ? names.khmerNames() : null;
        return corridors.stream()
                .map(corridor -> {
                    SearchHitResponse hit = mapper.toHit(corridor, reviews.get(corridor.id()));
                    return khmer ? mapper.inKhmer(hit, corridor.nameKm(), corridor.shortDescriptionKm(), words) : hit;
                })
                .toList();
    }

    private static SuggestionResponse suggestion(SuggestionKind kind, UUID id, String name, String nameKm, String slug, boolean khmer) {
        if (!khmer) {
            return new SuggestionResponse(kind, id, name, slug, List.of());
        }
        boolean written = nameKm != null && !nameKm.isBlank();
        return new SuggestionResponse(kind, id, written ? nameKm : name, slug, written ? List.of() : List.of("name"));
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
                                                           Map<UUID, String> provinceNames, Map<UUID, String> provinceNamesKm) {
        Map<UUID, List<T>> byProvince = new HashMap<>();
        for (T row : rows) {
            for (UUID provinceId : provincesOf.apply(row)) {
                if (provinceId != null) {
                    byProvince.computeIfAbsent(provinceId, id -> new ArrayList<>()).add(row);
                }
            }
        }
        return byProvince.entrySet().stream()
                .map(group -> {
                    String khmer = provinceNamesKm == null ? null : provinceNamesKm.get(group.getKey());
                    boolean english = provinceNamesKm != null && khmer == null;
                    return new ProvinceGroupResponse(group.getKey(), khmer != null ? khmer : provinceNames.get(group.getKey()),
                            group.getValue().size(), hits.apply(group.getValue().subList(0, Math.min(perProvince, group.getValue().size()))),
                            english ? List.of("provinceName") : List.of());
                })
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
