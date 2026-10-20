package ams.asmtravel.api.services.destination.food.service.impl;

import ams.asmtravel.api.services.location.province.service.ProvinceService;
import ams.asmtravel.api.services.taxonomy.category.domain.CategoryDeletingEvent;
import ams.asmtravel.api.services.taxonomy.category.service.CategoryService;
import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.domain.KhmerText;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.destination.exception.KhmerTextMissingException;
import ams.asmtravel.api.services.destination.destination.domain.NearbyArea;
import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.destination.food.domain.Food;
import ams.asmtravel.api.services.destination.food.domain.FoodAmenities;
import ams.asmtravel.api.services.destination.food.domain.FoodDeletingEvent;
import ams.asmtravel.api.services.destination.food.domain.FoodSavedEvent;
import ams.asmtravel.api.services.destination.food.domain.FoodSpecialties;
import ams.asmtravel.api.services.destination.food.domain.FoodVocabularies;
import ams.asmtravel.api.services.destination.food.dto.request.FoodRequest;
import ams.asmtravel.api.services.destination.food.dto.response.FoodResponse;
import ams.asmtravel.api.services.destination.food.exception.FoodNotFoundException;
import ams.asmtravel.api.services.destination.food.mapper.FoodMapper;
import ams.asmtravel.api.services.destination.food.repository.FoodRepository;
import ams.asmtravel.api.services.destination.food.security.FoodPermissionEvaluator;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.taxonomy.interest.domain.InterestDeletingEvent;
import ams.asmtravel.api.services.taxonomy.interest.service.InterestService;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.taxonomy.tag.domain.TagDeletingEvent;
import ams.asmtravel.api.services.taxonomy.tag.service.TagService;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FoodServiceImpl implements FoodService {

    /** Listings shown on a destination page, within {@link NearbyArea#LISTING_RADIUS_KM}. */
    static final int NEAR_LIMIT = 12;

    private final FoodRepository foodRepository;
    private final DestinationService destinationService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final InterestService interestService;
    private final ProvinceService provinceService;
    private final FoodPermissionEvaluator permission;
    private final FoodMapper mapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<FoodResponse> getAll(UUID destinationId, String slug) {
        boolean everything = Authorities.isBackoffice(Authorities.current());
        List<Food> foods;
        if (slug != null && !slug.isBlank()) {
            foods = foodRepository.findBySlug(slug.trim()).stream().toList();
        } else if (destinationId != null) {
            foods = foodRepository.findByDestinationIdOrderByNameAsc(destinationId);
        } else {
            foods = everything ? foodRepository.findAllByOrderByNameAsc()
                    : foodRepository.findByStatusOrderByNameAsc(ListingStatus.PUBLISHED);
        }
        return foods.stream()
                .filter(food -> everything || food.getStatus() == ListingStatus.PUBLISHED)
                .map(mapper::toFoodResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodResponse> getNear(UUID destinationId) {
        NearbyArea area = NearbyArea.around(destinationService.getEntity(destinationId)).orElse(null);
        if (area == null) {
            return List.of();
        }
        List<Food> candidates = foodRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                ListingStatus.PUBLISHED, area.south(), area.north(), area.west(), area.east());

        record Near(Food food, double km) {
        }
        return candidates.stream()
                .map(food -> new Near(food, area.kilometresTo(food.getLatitude(), food.getLongitude())))
                .filter(near -> near.km() <= area.radiusKm())
                .sorted(Comparator.comparingDouble(Near::km).thenComparing(near -> near.food().getName()))
                .limit(NEAR_LIMIT)
                .map(near -> mapper.toFoodResponse(near.food()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FoodResponse findById(UUID id) {
        Food food = find(id);
        if (food.getStatus() != ListingStatus.PUBLISHED && !Authorities.isBackoffice(Authorities.current())) {
            throw new FoodNotFoundException();
        }
        return mapper.toFoodResponse(food);
    }

    @Override
    @Transactional
    public FoodResponse create(FoodRequest request, UUID authorId) {
        UUID ownerUserId = owner(request.ownerUserId());
        checkWrite(ownerUserId);
        Food food = new Food();
        food.setSlug(slugFor(request.slug(), request.name(), null));
        food.setCreatedBy(authorId);
        apply(food, request, ownerUserId);
        setStatus(food, permittedStatus(request.status(), ListingStatus.DRAFT));
        return mapper.toFoodResponse(foodRepository.save(food));
    }

    @Override
    @Transactional
    public FoodResponse update(UUID id, FoodRequest request) {
        Food food = find(id);
        checkWrite(food.getOwnerUserId());
        checkChange(food.getStatus());
        UUID ownerUserId = owner(request.ownerUserId());
        checkWrite(ownerUserId);
        if (request.slug() != null && !request.slug().isBlank() && !request.slug().equals(food.getSlug())) {
            food.setSlug(slugFor(request.slug(), request.name(), id));
        }
        apply(food, request, ownerUserId);
        setStatus(food, permittedStatus(request.status(), food.getStatus()));
        Food saved = foodRepository.saveAndFlush(food);
        events.publishEvent(new FoodSavedEvent(saved.getId()));
        return mapper.toFoodResponse(saved);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Food food = find(id);
        if (!permission.canDelete(food.getOwnerUserId(), food.getStatus(), Authorities.current())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete this listing");
        }
        remove(food);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPublished(UUID id) {
        return id != null && foodRepository.findById(id).map(food -> food.getStatus() == ListingStatus.PUBLISHED)
                .orElse(false);
    }

    @Override
    @Transactional
    public FoodResponse publish(UUID id) {
        Food food = find(id);
        setStatus(food, ListingStatus.PUBLISHED);
        return mapper.toFoodResponse(foodRepository.saveAndFlush(food));
    }

    @Override
    @Transactional
    public FoodResponse unpublish(UUID id) {
        Food food = find(id);
        setStatus(food, ListingStatus.DRAFT);
        return mapper.toFoodResponse(foodRepository.saveAndFlush(food));
    }

    @Override
    @Transactional
    public void updateRating(UUID id, BigDecimal average, int count) {
        foodRepository.updateRating(id, average, count);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return foodRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Food getEntity(UUID id) {
        return foodRepository.findById(id).orElseThrow(() -> new FoodNotFoundException());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return id != null && foodRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canWrite(UUID id, Authentication authentication) {
        return foodRepository.findById(id)
                .map(food -> permission.canWrite(food.getOwnerUserId(), authentication)
                        && permission.canChange(food.getStatus(), authentication))
                .orElse(false);
    }

    @Override
    @Transactional
    public void setPriceFrom(UUID id, BigDecimal priceFrom) {
        Food food = getEntity(id);
        food.setPriceFrom(priceFrom);
        foodRepository.save(food);
    }

    /** The gallery picks the cover; the listing only stores it. */
    @Override
    @Transactional
    public void setCover(UUID id, String coverKey) {
        Food food = getEntity(id);
        food.setCoverKey(coverKey);
        foodRepository.save(food);
    }

    /** A deleted destination is only unlinked; the listing stays. */
    @EventListener
    public void onDestinationDeleting(DestinationDeletingEvent event) {
        foodRepository.releaseDestination(event.destinationId());
    }

    /** A listing outlives its owner's account. */
    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        foodRepository.releaseOwner(event.userId());
    }

    @EventListener
    public void onCategoryDeleting(CategoryDeletingEvent event) {
        foodRepository.unlinkCategory(event.categoryId());
    }

    @EventListener
    public void onTagDeleting(TagDeletingEvent event) {
        foodRepository.unlinkTag(event.tagId());
    }

    @EventListener
    public void onInterestDeleting(InterestDeletingEvent event) {
        foodRepository.unlinkInterest(event.interestId());
    }

    private void remove(Food food) {
        events.publishEvent(new FoodDeletingEvent(food.getId()));
        foodRepository.delete(food);
    }

    private void apply(Food food, FoodRequest request, UUID ownerUserId) {
        if (request.destinationId() != null && !destinationService.exists(request.destinationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such destination");
        }
        food.setDestination(request.destinationId() == null ? null : destinationService.getEntity(request.destinationId()));
        food.setOwnerUserId(ownerUserId);
        food.setName(request.name().trim());
        food.setType(request.type());
        food.setShortDescription(blankToNull(request.shortDescription()));
        food.setDescription(blankToNull(request.description()));
        food.setSlogan(blankToNull(request.slogan()));
        food.setSloganBy(blankToNull(request.sloganBy()));
        /* Khmer: null keeps it, so a client that has never heard of it wipes nothing. */
        if (request.nameKm() != null) {
            food.setNameKm(blankToNull(request.nameKm()));
        }
        if (request.descriptionKm() != null) {
            food.setDescriptionKm(KhmerText.hasText(request.descriptionKm()) ? request.descriptionKm().trim() : null);
        }
        if (request.sloganKm() != null) {
            food.setSloganKm(blankToNull(request.sloganKm()));
        }
        if (request.sloganByKm() != null) {
            food.setSloganByKm(blankToNull(request.sloganByKm()));
        }
        if (request.addressKm() != null) {
            food.setAddressKm(blankToNull(request.addressKm()));
        }
        food.setProvince(request.provinceId() == null ? null : provinceService.getEntity(request.provinceId()));
        food.setAddress(blankToNull(request.address()));
        food.setLatitude(request.latitude());
        food.setLongitude(request.longitude());
        food.setPriceRange(blankToNull(request.priceRange()));
        food.setAverageMealPrice(request.averageMealPrice());
        food.setPriceFrom(request.priceFrom());
        food.setCurrency(request.currency() == null || request.currency().isBlank() ? "USD" : request.currency());
        food.setOpeningHours(request.openingHours() == null ? new LinkedHashMap<>()
                : new LinkedHashMap<>(request.openingHours()));
        food.setPhone(blankToNull(request.phone()));
        food.setContactEmail(blankToNull(request.contactEmail()));
        food.setWebsite(blankToNull(request.website()));
        food.setCoverKey(blankToNull(request.coverKey()));
        food.setCuisines(chosen(request.cuisines(), FoodVocabularies.CUISINES, "cuisine"));
        food.setMealsServed(chosen(request.mealsServed(), FoodVocabularies.MEALS, "meal"));
        food.setDietary(chosen(request.dietary(), FoodVocabularies.DIETARY, "dietary option"));
        food.setServiceOptions(chosen(request.serviceOptions(), FoodVocabularies.SERVICE_OPTIONS, "service"));
        food.setGoodFor(chosen(request.goodFor(), FoodVocabularies.GOOD_FOR, "occasion"));
        food.setMenuLanguages(chosen(request.menuLanguages(), FoodVocabularies.LANGUAGES, "menu language"));
        food.setDeliveryPartners(chosen(request.deliveryPartners(), FoodVocabularies.DELIVERY_PARTNERS, "delivery partner"));
        food.setPaymentMethods(chosen(request.paymentMethods(), FoodVocabularies.PAYMENT_METHODS, "payment method"));
        food.setLanguagesSpoken(chosen(request.languagesSpoken(), FoodVocabularies.LANGUAGES, "language"));
        /* Fixed list so it can be filtered. */
        List<String> amenities = clean(request.amenities());
        List<String> unknownAmenities = FoodAmenities.unknown(amenities);
        if (!unknownAmenities.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No such facility: " + String.join(", ", unknownAmenities));
        }
        food.setAmenities(new ArrayList<>(amenities));
        /* Checked against every kind's list, so a listing that changed type keeps its values. */
        List<String> specialties = clean(request.specialties());
        List<String> unknownSpecialties = FoodSpecialties.unknown(specialties);
        if (!unknownSpecialties.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No such specialty: " + String.join(", ", unknownSpecialties));
        }
        food.setSpecialties(new ArrayList<>(specialties));
        food.setAlcohol(request.alcohol());
        food.setParkingOption(request.parkingOption());
        food.setReservations(request.reservations());
        food.setSeatingCapacity(request.seatingCapacity());
        food.setTelegram(blankToNull(request.telegram()));
        food.setWhatsapp(blankToNull(request.whatsapp()));
        food.setFacebookUrl(blankToNull(request.facebookUrl()));
        food.setYearOpened(request.yearOpened());
        if (request.isLocalSpecialty() != null) {
            food.setIsLocalSpecialty(request.isLocalSpecialty());
        }
        if (request.categoryIds() != null || food.getId() == null) {
            food.setCategories(new HashSet<>(categoryService.getEntities(nullToEmpty(request.categoryIds()))));
        }
        if (request.tagIds() != null || food.getId() == null) {
            food.setTags(new HashSet<>(tagService.getEntities(nullToEmpty(request.tagIds()))));
        }
        if (request.interestIds() != null || food.getId() == null) {
            food.setInterests(new HashSet<>(interestService.getEntities(nullToEmpty(request.interestIds()))));
        }
    }

    /** Trims and de-duplicates values; refuses any value not in {@code allowed}. */
    private static List<String> chosen(List<String> values, List<String> allowed, String what) {
        List<String> clean = clean(values);
        List<String> unknown = FoodVocabularies.unknown(clean, allowed);
        if (!unknown.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such " + what + ": " + String.join(", ", unknown));
        }
        return new ArrayList<>(clean);
    }

    private static List<String> clean(List<String> values) {
        return values == null ? List.of()
                : values.stream().map(String::trim).filter(one -> !one.isEmpty()).distinct().toList();
    }

    /** Refused with 409 while any filled English field has no Khmer. */
    @Override
    @Transactional(readOnly = true)
    public void requireKhmer(UUID id) {
        Food food = find(id);
        if (!food.missingKhmer().isEmpty()) {
            throw new KhmerTextMissingException(food.missingKhmer());
        }
    }

    /** On its way live, only with its Khmer written. */
    private void setStatus(Food food, ListingStatus status) {
        if (status == ListingStatus.PUBLISHED && food.getStatus() != ListingStatus.PUBLISHED && !food.missingKhmer().isEmpty()) {
            throw new KhmerTextMissingException(food.missingKhmer());
        }
        if (status == ListingStatus.PUBLISHED && food.getStatus() != ListingStatus.PUBLISHED) {
            food.setPublishedAt(Instant.now());
        }
        food.setStatus(status);
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This listing is live: an editor makes changes to it now");
        }
    }

    private void checkWrite(UUID ownerUserId) {
        if (!permission.canWrite(ownerUserId, Authorities.current())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ownerUserId == null
                    ? "Name the account this listing belongs to"
                    : "This listing is listed in someone else's name");
        }
    }

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

    private String slugFor(String wanted, String name, UUID id) {
        if (wanted != null && !wanted.isBlank()) {
            boolean taken = id == null ? foodRepository.existsBySlug(wanted) : foodRepository.existsBySlugAndIdNot(wanted, id);
            if (taken) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another listing already uses the slug " + wanted);
            }
            return wanted;
        }
        return Slugs.unique(Slugs.of(name), foodRepository::existsBySlug);
    }

    private Food find(UUID id) {
        return foodRepository.findWithLinksById(id).orElseThrow(() -> new FoodNotFoundException());
    }

    private static <T> Set<T> nullToEmpty(Set<T> values) {
        return values == null ? Set.of() : values;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
