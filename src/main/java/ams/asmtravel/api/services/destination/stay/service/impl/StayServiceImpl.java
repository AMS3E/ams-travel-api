package ams.asmtravel.api.services.destination.stay.service.impl;

import ams.asmtravel.api.services.location.province.service.ProvinceService;
import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.destination.domain.NearbyArea;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.destination.stay.domain.Stay;
import ams.asmtravel.api.services.destination.stay.domain.StaySpecialties;
import ams.asmtravel.api.services.destination.stay.domain.StayDeletingEvent;
import ams.asmtravel.api.services.destination.stay.domain.StayVocabularies;
import ams.asmtravel.api.services.destination.stay.domain.StaySavedEvent;
import ams.asmtravel.api.services.destination.stay.dto.request.StayRequest;
import ams.asmtravel.api.services.destination.stay.dto.response.StayResponse;
import ams.asmtravel.api.services.destination.stay.exception.StayNotFoundException;
import ams.asmtravel.api.services.destination.stay.mapper.StayMapper;
import ams.asmtravel.api.services.destination.stay.repository.StayRepository;
import ams.asmtravel.api.services.destination.stay.security.StayPermissionEvaluator;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.taxonomy.amenity.domain.AmenityDeletingEvent;
import ams.asmtravel.api.services.taxonomy.amenity.domain.AmenityLevel;
import ams.asmtravel.api.services.taxonomy.amenity.service.AmenityService;
import ams.asmtravel.api.services.taxonomy.interest.domain.InterestDeletingEvent;
import ams.asmtravel.api.services.taxonomy.interest.service.InterestService;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.authorization.domain.Authorities;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StayServiceImpl implements StayService {

    /** Stays listed on a destination page, within {@link NearbyArea#LISTING_RADIUS_KM}. */
    static final int NEAR_LIMIT = 12;

    private final StayRepository stayRepository;
    private final AmenityService amenityService;
    private final DestinationService destinationService;
    private final UserService userService;
    private final InterestService interestService;
    private final ProvinceService provinceService;
    private final StayPermissionEvaluator permission;
    private final StayMapper mapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<StayResponse> getAll(UUID destinationId) {
        boolean everything = Authorities.isBackoffice(Authorities.current());
        List<Stay> stays;
        if (destinationId == null) {
            stays = everything ? stayRepository.findAllByOrderByNameAsc()
                    : stayRepository.findByStatusOrderByNameAsc(ListingStatus.PUBLISHED);
        } else {
            stays = everything ? stayRepository.findByDestinationIdOrderByNameAsc(destinationId)
                    : stayRepository.findByDestinationIdAndStatusOrderByNameAsc(destinationId, ListingStatus.PUBLISHED);
        }
        return stays.stream().map(mapper::toStayResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StayResponse> getNear(UUID destinationId) {
        NearbyArea area = NearbyArea.around(destinationService.getEntity(destinationId)).orElse(null);
        if (area == null) {
            return List.of();
        }
        List<Stay> candidates = stayRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                ListingStatus.PUBLISHED, area.south(), area.north(), area.west(), area.east());

        record Near(Stay stay, double km) {
        }
        return candidates.stream()
                .map(stay -> new Near(stay, area.kilometresTo(stay.getLatitude(), stay.getLongitude())))
                .filter(near -> near.km() <= area.radiusKm())
                .sorted(Comparator.comparingDouble(Near::km).thenComparing(near -> near.stay().getName()))
                .limit(NEAR_LIMIT)
                .map(near -> mapper.toStayResponse(near.stay()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StayResponse findById(UUID id) {
        Stay stay = find(id);
        if (stay.getStatus() != ListingStatus.PUBLISHED && !Authorities.isBackoffice(Authorities.current())) {
            throw new StayNotFoundException();
        }
        return mapper.toStayResponse(stay);
    }

    @Override
    @Transactional
    public StayResponse create(StayRequest request) {
        UUID ownerUserId = owner(request.ownerUserId());
        checkWrite(ownerUserId);
        Stay stay = new Stay();
        apply(stay, request, ownerUserId);
        stay.setStatus(permittedStatus(request.status(), ListingStatus.DRAFT));
        return mapper.toStayResponse(stayRepository.save(stay));
    }

    @Override
    @Transactional
    public StayResponse update(UUID id, StayRequest request) {
        Stay stay = find(id);
        checkWrite(stay.getOwnerUserId());
        checkChange(stay.getStatus());
        UUID ownerUserId = owner(request.ownerUserId());
        checkWrite(ownerUserId);
        apply(stay, request, ownerUserId);
        stay.setStatus(permittedStatus(request.status(), stay.getStatus()));
        Stay saved = stayRepository.saveAndFlush(stay);
        // Rooms reset the "from" price before the response is built.
        events.publishEvent(new StaySavedEvent(saved.getId()));
        return mapper.toStayResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Stay stay = find(id);
        if (!permission.canDelete(stay.getOwnerUserId(), stay.getStatus(), Authorities.current())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete this stay");
        }
        remove(stay);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPublished(UUID id) {
        return id != null && stayRepository.findById(id).map(stay -> stay.getStatus() == ListingStatus.PUBLISHED)
                .orElse(false);
    }

    @Override
    @Transactional
    public StayResponse publish(UUID id) {
        Stay stay = find(id);
        stay.setStatus(ListingStatus.PUBLISHED);
        return mapper.toStayResponse(stayRepository.saveAndFlush(stay));
    }

    @Override
    @Transactional
    public StayResponse unpublish(UUID id) {
        Stay stay = find(id);
        stay.setStatus(ListingStatus.DRAFT);
        return mapper.toStayResponse(stayRepository.saveAndFlush(stay));
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return stayRepository.count();
    }

    /** A deleted destination is only unlinked; the stay stays. */
    @EventListener
    public void onDestinationDeleting(DestinationDeletingEvent event) {
        stayRepository.releaseDestination(event.destinationId());
    }

    /** An amenity stays still list cannot be deleted: retire it instead. */
    @EventListener
    public void onAmenityDeleting(AmenityDeletingEvent event) {
        long listing = stayRepository.countByAmenity("%\"" + event.slug() + "\"%");
        if (listing > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    listing + (listing == 1 ? " stay still lists it" : " stays still list it") + ": retire the amenity instead of deleting it");
        }
    }

    @EventListener
    public void onInterestDeleting(InterestDeletingEvent event) {
        stayRepository.unlinkInterest(event.interestId());
    }

    /** A stay outlives its owner's account. */
    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        stayRepository.releaseOwner(event.userId());
    }

    private void remove(Stay stay) {
        events.publishEvent(new StayDeletingEvent(stay.getId()));
        stayRepository.delete(stay);
    }

    @Override
    @Transactional(readOnly = true)
    public Stay getEntity(UUID id) {
        return stayRepository.findById(id).orElseThrow(StayNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return id != null && stayRepository.existsById(id);
    }

    @Override
    @Transactional
    public void recordView(UUID id) {
        stayRepository.incrementViewCount(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canWrite(UUID id, Authentication authentication) {
        return stayRepository.findById(id)
                .map(stay -> permission.canWrite(stay.getOwnerUserId(), authentication)
                        && permission.canChange(stay.getStatus(), authentication))
                .orElse(false);
    }

    @Override
    @Transactional
    public void setPriceFrom(UUID id, BigDecimal priceFrom) {
        Stay stay = getEntity(id);
        stay.setPriceFrom(priceFrom);
        stayRepository.save(stay);
    }

    /** The gallery picks the cover; the listing only stores it. */
    @Override
    @Transactional
    public void setCover(UUID id, String coverKey) {
        Stay stay = getEntity(id);
        stay.setCoverKey(coverKey);
        stayRepository.save(stay);
    }

    private void apply(Stay stay, StayRequest request, UUID ownerUserId) {
        if (request.destinationId() != null && !destinationService.exists(request.destinationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such destination");
        }
        stay.setDestination(request.destinationId() == null ? null : destinationService.getEntity(request.destinationId()));
        stay.setOwnerUserId(ownerUserId);
        stay.setName(request.name().trim());
        stay.setType(request.type());
        stay.setStarRating(request.starRating());
        stay.setShortDescription(blankToNull(request.shortDescription()));
        stay.setDescription(request.description());
        stay.setSlogan(blankToNull(request.slogan()));
        stay.setSloganBy(blankToNull(request.sloganBy()));
        stay.setProvince(request.provinceId() == null ? null : provinceService.getEntity(request.provinceId()));
        stay.setPolicies(blankToNull(request.policies()));
        stay.setCheckInFrom(request.checkInFrom());
        stay.setCheckOutUntil(request.checkOutUntil());
        stay.setPhone(blankToNull(request.phone()));
        stay.setContactEmail(blankToNull(request.contactEmail()));
        stay.setWebsite(blankToNull(request.website()));
        stay.setPriceFrom(request.priceFrom());
        stay.setCurrency(request.currency() == null || request.currency().isBlank() ? "USD" : request.currency());
        /* Amenities are the taxonomy's, offered at the property; unknown slugs are refused. */
        List<String> amenities = request.amenities() == null ? List.of()
                : request.amenities().stream().map(String::trim).filter(one -> !one.isEmpty()).distinct().toList();
        List<String> unknown = amenityService.unknown(amenities, AmenityLevel.PROPERTY);
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such amenity: " + String.join(", ", unknown));
        }
        stay.setAmenities(new ArrayList<>(amenities));
        /* Checked against every kind's list, so a stay that changed type keeps its values. */
        List<String> specialties = request.specialties() == null ? List.of()
                : request.specialties().stream().map(String::trim).filter(one -> !one.isEmpty()).distinct().toList();
        List<String> unknownSpecialties = StaySpecialties.unknown(specialties);
        if (!unknownSpecialties.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No such specialty: " + String.join(", ", unknownSpecialties));
        }
        stay.setSpecialties(new ArrayList<>(specialties));
        /* Null keeps the weekend nights (Friday and Saturday on a new stay); an empty list means no night costs more. */
        if (request.weekendNights() != null) {
            stay.setWeekendNights(new ArrayList<>(request.weekendNights().stream().map(night -> night.trim().toUpperCase(Locale.ROOT)).distinct().toList()));
        }
        stay.setCoverKey(request.coverKey() == null || request.coverKey().isBlank() ? null : request.coverKey());
        stay.setAddress(request.address() == null || request.address().isBlank() ? null : request.address().trim());
        stay.setLatitude(request.latitude());
        stay.setLongitude(request.longitude());

        /* House rules, services and payment. */
        stay.setCheckInUntil(request.checkInUntil());
        stay.setCheckOutFrom(request.checkOutFrom());
        stay.setMinCheckInAge(request.minCheckInAge());
        stay.setChildrenAllowed(!Boolean.FALSE.equals(request.childrenAllowed()));
        stay.setPetsPolicy(request.petsPolicy());
        stay.setSmokingPolicy(request.smokingPolicy());
        stay.setPartiesAllowed(Boolean.TRUE.equals(request.partiesAllowed()));
        stay.setQuietHours(blankToNull(request.quietHours()));
        stay.setBreakfastOption(request.breakfastOption());
        stay.setBreakfastTypes(chosen(request.breakfastTypes(), StayVocabularies.BREAKFAST_TYPES, "breakfast"));
        stay.setBreakfastPrice(request.breakfastPrice());
        stay.setParkingOption(request.parkingOption());
        stay.setParkingPrice(request.parkingPrice());
        stay.setPaymentMethods(chosen(request.paymentMethods(), StayVocabularies.PAYMENT_METHODS, "payment method"));
        stay.setLanguagesSpoken(chosen(request.languagesSpoken(), StayVocabularies.LANGUAGES, "language"));
        stay.setTelegram(blankToNull(request.telegram()));
        stay.setWhatsapp(blankToNull(request.whatsapp()));
        stay.setFacebookUrl(blankToNull(request.facebookUrl()));
        stay.setLicenseNumber(blankToNull(request.licenseNumber()));
        stay.setYearOpened(request.yearOpened());
        if (request.interestIds() != null || stay.getId() == null) {
            Set<UUID> interestIds = request.interestIds() == null ? Set.of() : request.interestIds();
            stay.setInterests(new HashSet<>(interestService.getEntities(interestIds)));
        }
    }

    /** Trims and de-duplicates values; refuses any value not in {@code allowed}. */
    private static List<String> chosen(List<String> values, List<String> allowed, String what) {
        List<String> clean = values == null ? List.of()
                : values.stream().map(String::trim).filter(one -> !one.isEmpty()).distinct().toList();
        List<String> unknown = StayVocabularies.unknown(clean, allowed);
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such " + what + ": " + String.join(", ", unknown));
        }
        return new ArrayList<>(clean);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** An editor may name any owner; a partner only themselves (empty means themselves). */
    private UUID owner(UUID requested) {
        Authentication caller = Authorities.current();
        if (requested == null) {
            return permission.isEditor(caller) ? null : userService.findUserId(caller).orElse(null);
        }
        if (!userService.exists(requested)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such account");
        }
        return requested;
    }

    private void checkChange(ListingStatus status) {
        if (!permission.canChange(status, Authorities.current())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This stay is live: an editor makes changes to it now");
        }
    }

    private void checkWrite(UUID ownerUserId) {
        if (!permission.canWrite(ownerUserId, Authorities.current())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ownerUserId == null
                    ? "Name the account this stay belongs to"
                    : "This stay is listed in someone else's name");
        }
    }

    /** Only an editor may publish or unpublish; other writers keep the other statuses. */
    private ListingStatus permittedStatus(ListingStatus requested, ListingStatus current) {
        if (requested == null || requested == current) {
            return current;
        }
        if ((requested == ListingStatus.PUBLISHED || current == ListingStatus.PUBLISHED)
                && !permission.isEditor(Authorities.current())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an editor can publish or unpublish a listing");
        }
        return requested;
    }

    private Stay find(UUID id) {
        return stayRepository.findById(id).orElseThrow(() -> new StayNotFoundException());
    }
}
