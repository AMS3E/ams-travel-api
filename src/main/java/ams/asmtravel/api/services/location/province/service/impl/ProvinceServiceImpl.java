package ams.asmtravel.api.services.location.province.service.impl;

import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.province.domain.ProvinceDeletingEvent;
import ams.asmtravel.api.services.location.province.domain.ProvinceStanding;
import ams.asmtravel.api.services.location.province.domain.ProvinceSummary;
import ams.asmtravel.api.services.location.province.dto.request.ProvinceRequest;
import ams.asmtravel.api.services.location.province.dto.response.ProvinceResponse;
import ams.asmtravel.api.services.location.province.exception.ProvinceNotFoundException;
import ams.asmtravel.api.services.location.province.mapper.ProvinceMapper;
import ams.asmtravel.api.services.location.province.repository.ProvinceRepository;
import ams.asmtravel.api.services.location.province.service.ProvinceService;
import ams.asmtravel.api.services.location.region.domain.Region;
import ams.asmtravel.api.services.location.region.domain.RegionDeletingEvent;
import ams.asmtravel.api.services.location.region.domain.RegionKind;
import ams.asmtravel.api.services.location.region.dto.response.GeoBounds;
import ams.asmtravel.api.services.location.region.service.RegionService;
import ams.asmtravel.api.services.review.review.domain.ReviewTargetType;
import ams.asmtravel.api.services.review.review.dto.response.ReviewSummaryResponse;
import ams.asmtravel.api.services.review.review.service.ReviewService;
import ams.asmtravel.api.services.user.user.service.UserService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ProvinceServiceImpl implements ProvinceService {

    private static final int SUMMARY_BATCH = 100;

    private final ProvinceRepository provinceRepository;
    private final RegionService regionService;
    private final ProvinceMapper mapper;
    private final UserService userService;
    private final ApplicationEventPublisher events;
    /* The listings and their reviews depend on this service for their province, so they are reached lazily. */
    private final ObjectProvider<DestinationService> destinationService;
    private final ObjectProvider<StayService> stayService;
    private final ObjectProvider<FoodService> foodService;
    private final ObjectProvider<ReviewService> reviewService;

    public ProvinceServiceImpl(ProvinceRepository provinceRepository, RegionService regionService, ProvinceMapper mapper,
                               UserService userService, ApplicationEventPublisher events,
                               ObjectProvider<DestinationService> destinationService, ObjectProvider<StayService> stayService,
                               ObjectProvider<FoodService> foodService, ObjectProvider<ReviewService> reviewService) {
        this.provinceRepository = provinceRepository;
        this.regionService = regionService;
        this.mapper = mapper;
        this.userService = userService;
        this.events = events;
        this.destinationService = destinationService;
        this.stayService = stayService;
        this.foodService = foodService;
        this.reviewService = reviewService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProvinceResponse> getAll() {
        List<Province> provinces = provinceRepository.findAllByOrderByNameAsc();
        Map<UUID, ProvinceStanding> standings = standings();
        return provinces.stream()
                .map(province -> mapper.toProvinceResponse(province, standings.getOrDefault(province.getId(), ProvinceStanding.NONE)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProvinceResponse findById(UUID id) {
        return respond(find(id));
    }

    @Override
    @Transactional
    public ProvinceResponse create(ProvinceRequest request) {
        Province province = new Province();
        province.setSlug(Slugs.forCreate(null, request.name(), provinceRepository::existsBySlug));
        apply(province, request);
        province.setIsActive(request.isActive() == null || request.isActive());
        province.setCreatedBy(author());
        return respond(provinceRepository.save(province));
    }

    /** A renamed province keeps the address it was published under. */
    @Override
    @Transactional
    public ProvinceResponse update(UUID id, ProvinceRequest request) {
        Province province = find(id);
        apply(province, request);
        if (request.isActive() != null) {
            province.setIsActive(request.isActive());
        }
        return respond(provinceRepository.saveAndFlush(province));
    }

    /** Refused while destinations are in it (by their service, then by the foreign key). */
    @Override
    @Transactional
    public void delete(UUID id) {
        Province province = find(id);
        events.publishEvent(new ProvinceDeletingEvent(id));
        provinceRepository.delete(province);
        try {
            provinceRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Destinations are still in this province; move them first");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProvinceSummary> findSummaries() {
        return provinceRepository.findAllByOrderByNameAsc().stream().map(mapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return id != null && provinceRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Province getEntity(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A province is required");
        }
        return provinceRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such province"));
    }

    @EventListener
    public void onRegionDeleting(RegionDeletingEvent event) {
        if (provinceRepository.existsByRegionId(event.regionId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This region still holds provinces; move them to another region first");
        }
    }

    /**
     * A zone is optional and a missing one keeps the current; a tourism region is never a zone.
     * The cover: null keeps it, empty removes it. The centre: both coordinates or neither; neither keeps it.
     */
    private void apply(Province province, ProvinceRequest request) {
        if (request.regionId() != null) {
            Region zone = regionService.getEntity(request.regionId());
            if (zone.getKind() == RegionKind.TOURISM) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        zone.getName() + " is a tourism region: a province belongs to one of the zones");
            }
            province.setRegion(zone);
        }
        province.setName(request.name().trim());
        if (request.nameKm() != null) {
            province.setNameKm(request.nameKm().isBlank() ? null : request.nameKm().trim());
        }
        province.setCountryCode(request.countryCode() == null || request.countryCode().isBlank()
                ? "KH" : request.countryCode().trim().toUpperCase(Locale.ROOT));
        if (request.coverKey() != null) {
            province.setCoverKey(request.coverKey().isBlank() ? null : request.coverKey().trim());
        }
        if (request.latitude() != null || request.longitude() != null) {
            if (request.latitude() == null || request.longitude() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A centre needs both a latitude and a longitude");
            }
            province.setLatitude(request.latitude());
            province.setLongitude(request.longitude());
        }
    }

    private ProvinceResponse respond(Province province) {
        return mapper.toProvinceResponse(province, standings().getOrDefault(province.getId(), ProvinceStanding.NONE));
    }

    /** The current account's id, or null for a token with no user (e.g. a service call). */
    private UUID author() {
        try {
            return userService.findUserId(Authorities.current()).orElse(null);
        } catch (ResponseStatusException noIdentity) {
            return null;
        }
    }

    private Province find(UUID id) {
        return provinceRepository.findById(id).orElseThrow(() -> new ProvinceNotFoundException());
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return provinceRepository.count();
    }

    // What each province's published places add up to.

    /** A published place of any kind, where it is and how it is rated. */
    private record Listed(UUID provinceId, GeoBounds.Point point, ReviewSummaryResponse reviews) {
    }

    private Map<UUID, ProvinceStanding> standings() {
        List<Listed> listed = new ArrayList<>();
        List<DestinationSummary> places = destinationService.getObject().findPublishedSummaries();
        Map<UUID, ReviewSummaryResponse> placeReviews = reviews(ReviewTargetType.DESTINATION, places.stream().map(DestinationSummary::id).toList());
        places.forEach(place -> listed.add(new Listed(place.provinceId(),
                new GeoBounds.Point(place.latitude(), place.longitude()), placeReviews.get(place.id()))));

        var stays = stayService.getObject().getAll(null).stream().filter(stay -> stay.status() == ListingStatus.PUBLISHED).toList();
        Map<UUID, ReviewSummaryResponse> stayReviews = reviews(ReviewTargetType.STAY, stays.stream().map(stay -> stay.id()).toList());
        stays.forEach(stay -> listed.add(new Listed(stay.provinceId(),
                new GeoBounds.Point(stay.latitude(), stay.longitude()), stayReviews.get(stay.id()))));

        var foods = foodService.getObject().getAll(null, null).stream().filter(food -> food.status() == ListingStatus.PUBLISHED).toList();
        Map<UUID, ReviewSummaryResponse> foodReviews = reviews(ReviewTargetType.FOOD, foods.stream().map(food -> food.id()).toList());
        foods.forEach(food -> listed.add(new Listed(food.provinceId(),
                new GeoBounds.Point(food.latitude(), food.longitude()), foodReviews.get(food.id()))));

        Map<UUID, List<Listed>> byProvince = new HashMap<>();
        for (Listed row : listed) {
            if (row.provinceId() != null) {
                byProvince.computeIfAbsent(row.provinceId(), id -> new ArrayList<>()).add(row);
            }
        }
        Map<UUID, ProvinceStanding> standings = new HashMap<>();
        byProvince.forEach((provinceId, rows) -> standings.put(provinceId, standing(rows)));
        return standings;
    }

    /** Ratings averaged by weight, so a place with 200 ratings counts for more than one with 2. */
    private static ProvinceStanding standing(List<Listed> rows) {
        long ratingCount = 0;
        long reviewCount = 0;
        BigDecimal weighted = BigDecimal.ZERO;
        for (Listed row : rows) {
            if (row.reviews() == null) {
                continue;
            }
            reviewCount += row.reviews().reviewCount();
            if (row.reviews().averageRating() != null && row.reviews().ratingCount() > 0) {
                ratingCount += row.reviews().ratingCount();
                weighted = weighted.add(row.reviews().averageRating().multiply(BigDecimal.valueOf(row.reviews().ratingCount())));
            }
        }
        BigDecimal average = ratingCount == 0 ? null : weighted.divide(BigDecimal.valueOf(ratingCount), 1, RoundingMode.HALF_UP);
        return new ProvinceStanding(rows.size(), average, reviewCount, GeoBounds.around(rows.stream().map(Listed::point).toList()));
    }

    private Map<UUID, ReviewSummaryResponse> reviews(ReviewTargetType type, Collection<UUID> ids) {
        Map<UUID, ReviewSummaryResponse> byId = new HashMap<>();
        List<UUID> all = List.copyOf(ids);
        for (int from = 0; from < all.size(); from += SUMMARY_BATCH) {
            reviewService.getObject().summaries(type, all.subList(from, Math.min(from + SUMMARY_BATCH, all.size())))
                    .forEach(summary -> byId.put(summary.targetId(), summary));
        }
        return byId;
    }
}
