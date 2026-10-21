package ams.asmtravel.api.services.destination.activity.service.impl;

import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.destination.activity.domain.Activity;
import ams.asmtravel.api.services.destination.activity.domain.ActivityDeletingEvent;
import ams.asmtravel.api.services.destination.activity.dto.request.ActivityRequest;
import ams.asmtravel.api.services.destination.activity.dto.response.ActivityResponse;
import ams.asmtravel.api.services.destination.activity.exception.ActivityNotFoundException;
import ams.asmtravel.api.services.destination.activity.mapper.ActivityMapper;
import ams.asmtravel.api.services.destination.activity.repository.ActivityRepository;
import ams.asmtravel.api.services.destination.activity.security.ActivityPermissionEvaluator;
import ams.asmtravel.api.services.destination.activity.service.ActivityService;
import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.domain.KhmerText;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.destination.domain.NearbyArea;
import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.location.province.domain.ProvinceDeletingEvent;
import ams.asmtravel.api.services.location.province.service.ProvinceService;
import ams.asmtravel.api.services.location.region.domain.RegionDeletingEvent;
import ams.asmtravel.api.services.location.region.service.RegionService;
import ams.asmtravel.api.services.taxonomy.category.domain.CategoryDeletingEvent;
import ams.asmtravel.api.services.taxonomy.category.service.CategoryService;
import ams.asmtravel.api.services.taxonomy.interest.domain.InterestDeletingEvent;
import ams.asmtravel.api.services.taxonomy.interest.service.InterestService;
import ams.asmtravel.api.services.taxonomy.tag.domain.TagDeletingEvent;
import ams.asmtravel.api.services.taxonomy.tag.service.TagService;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import ams.asmtravel.api.services.user.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    /** Activities shown on a destination page, within {@link NearbyArea#LISTING_RADIUS_KM}. */
    static final int NEAR_LIMIT = 12;

    private final ActivityRepository activityRepository;
    private final DestinationService destinationService;
    private final UserService userService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final InterestService interestService;
    private final RegionService regionService;
    private final ProvinceService provinceService;
    private final ActivityPermissionEvaluator permission;
    private final ActivityMapper mapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> getAll(UUID destinationId, String slug) {
        boolean everything = Authorities.isBackoffice(Authorities.current());
        List<Activity> activities;
        if (slug != null && !slug.isBlank()) {
            activities = activityRepository.findBySlug(slug.trim()).stream().toList();
        } else if (destinationId != null) {
            activities = activityRepository.findByDestinationIdOrderByNameAsc(destinationId);
        } else {
            activities = everything ? activityRepository.findAllByOrderByNameAsc()
                    : activityRepository.findByStatusOrderByNameAsc(ListingStatus.PUBLISHED);
        }
        return activities.stream()
                .filter(activity -> everything || activity.getStatus() == ListingStatus.PUBLISHED)
                .map(mapper::toActivityResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityResponse> getNear(UUID destinationId) {
        NearbyArea area = NearbyArea.around(destinationService.getEntity(destinationId)).orElse(null);
        if (area == null) {
            return List.of();
        }
        List<Activity> candidates = activityRepository.findByStatusAndLatitudeBetweenAndLongitudeBetween(
                ListingStatus.PUBLISHED, area.south(), area.north(), area.west(), area.east());

        record Near(Activity activity, double km) {
        }
        return candidates.stream()
                .map(activity -> new Near(activity, area.kilometresTo(activity.getLatitude(), activity.getLongitude())))
                .filter(near -> near.km() <= area.radiusKm())
                .sorted(Comparator.comparingDouble(Near::km).thenComparing(near -> near.activity().getName()))
                .limit(NEAR_LIMIT)
                .map(near -> mapper.toActivityResponse(near.activity()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityResponse findById(UUID id) {
        Activity activity = find(id);
        if (activity.getStatus() != ListingStatus.PUBLISHED && !Authorities.isBackoffice(Authorities.current())) {
            throw new ActivityNotFoundException();
        }
        return mapper.toActivityResponse(activity);
    }

    @Override
    @Transactional
    public ActivityResponse create(ActivityRequest request, UUID authorId) {
        UUID ownerUserId = owner(request.ownerUserId());
        checkWrite(ownerUserId);
        Activity activity = new Activity();
        activity.setSlug(slugFor(request.slug(), request.name(), null));
        activity.setCreatedBy(authorId);
        apply(activity, request, ownerUserId);
        setStatus(activity, permittedStatus(request.status(), ListingStatus.DRAFT));
        return mapper.toActivityResponse(activityRepository.save(activity));
    }

    @Override
    @Transactional
    public ActivityResponse update(UUID id, ActivityRequest request) {
        Activity activity = find(id);
        checkWrite(activity.getOwnerUserId());
        checkChange(activity.getStatus());
        UUID ownerUserId = owner(request.ownerUserId());
        checkWrite(ownerUserId);
        if (request.slug() != null && !request.slug().isBlank() && !request.slug().equals(activity.getSlug())) {
            activity.setSlug(slugFor(request.slug(), request.name(), id));
        }
        apply(activity, request, ownerUserId);
        setStatus(activity, permittedStatus(request.status(), activity.getStatus()));
        return mapper.toActivityResponse(activityRepository.saveAndFlush(activity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Activity activity = find(id);
        if (!permission.canDelete(activity.getOwnerUserId(), activity.getStatus(), Authorities.current())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete this activity");
        }
        events.publishEvent(new ActivityDeletingEvent(activity.getId()));
        activityRepository.delete(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPublished(UUID id) {
        return id != null && activityRepository.findById(id)
                .map(activity -> activity.getStatus() == ListingStatus.PUBLISHED).orElse(false);
    }

    @Override
    @Transactional
    public ActivityResponse publish(UUID id) {
        Activity activity = find(id);
        setStatus(activity, ListingStatus.PUBLISHED);
        return mapper.toActivityResponse(activityRepository.saveAndFlush(activity));
    }

    @Override
    @Transactional
    public ActivityResponse unpublish(UUID id) {
        Activity activity = find(id);
        setStatus(activity, ListingStatus.DRAFT);
        return mapper.toActivityResponse(activityRepository.saveAndFlush(activity));
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return activityRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Activity getEntity(UUID id) {
        return activityRepository.findById(id).orElseThrow(ActivityNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return id != null && activityRepository.existsById(id);
    }

    @Override
    @Transactional
    public void recordView(UUID id) {
        activityRepository.incrementViewCount(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canWrite(UUID id, Authentication authentication) {
        return activityRepository.findById(id)
                .map(activity -> permission.canWrite(activity.getOwnerUserId(), authentication)
                        && permission.canChange(activity.getStatus(), authentication))
                .orElse(false);
    }

    /** The gallery picks the cover; the activity only stores it. */
    @Override
    @Transactional
    public void setCover(UUID id, String coverKey) {
        Activity activity = getEntity(id);
        activity.setCoverKey(coverKey);
        activityRepository.save(activity);
    }

    /** A deleted attraction is only unlinked; the activity stays. */
    @EventListener
    public void onDestinationDeleting(DestinationDeletingEvent event) {
        activityRepository.releaseDestination(event.destinationId());
    }

    /** An activity outlives its owner's account. */
    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        activityRepository.releaseOwner(event.userId());
    }

    /** A province is required, so one with activities cannot go until they have moved. */
    @EventListener
    public void onProvinceDeleting(ProvinceDeletingEvent event) {
        if (activityRepository.existsByProvinceId(event.provinceId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This province still has activities; move them first");
        }
    }

    @EventListener
    public void onRegionDeleting(RegionDeletingEvent event) {
        activityRepository.unlinkRegion(event.regionId());
    }

    @EventListener
    public void onCategoryDeleting(CategoryDeletingEvent event) {
        activityRepository.unlinkCategory(event.categoryId());
    }

    @EventListener
    public void onTagDeleting(TagDeletingEvent event) {
        activityRepository.unlinkTag(event.tagId());
    }

    @EventListener
    public void onInterestDeleting(InterestDeletingEvent event) {
        activityRepository.unlinkInterest(event.interestId());
    }

    private void apply(Activity activity, ActivityRequest request, UUID ownerUserId) {
        if (request.destinationId() != null && !destinationService.exists(request.destinationId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such destination");
        }
        activity.setDestination(request.destinationId() == null ? null : destinationService.getEntity(request.destinationId()));
        activity.setOwnerUserId(ownerUserId);
        activity.setName(request.name().trim());
        activity.setType(request.type());
        activity.setShortDescription(blankToNull(request.shortDescription()));
        activity.setDescription(blankToNull(request.description()));
        activity.setSlogan(blankToNull(request.slogan()));
        activity.setSloganBy(blankToNull(request.sloganBy()));
        /* Khmer: null keeps it, so a client that has never heard of it wipes nothing. */
        if (request.nameKm() != null) {
            activity.setNameKm(blankToNull(request.nameKm()));
        }
        if (request.descriptionKm() != null) {
            activity.setDescriptionKm(KhmerText.hasText(request.descriptionKm()) ? request.descriptionKm().trim() : null);
        }
        if (request.sloganKm() != null) {
            activity.setSloganKm(blankToNull(request.sloganKm()));
        }
        if (request.sloganByKm() != null) {
            activity.setSloganByKm(blankToNull(request.sloganByKm()));
        }
        if (request.addressKm() != null) {
            activity.setAddressKm(blankToNull(request.addressKm()));
        }
        activity.setProvince(provinceService.getEntity(request.provinceId()));
        activity.setAddress(blankToNull(request.address()));
        activity.setLatitude(request.latitude());
        activity.setLongitude(request.longitude());
        activity.setPhone(blankToNull(request.phone()));
        activity.setContactEmail(blankToNull(request.contactEmail()));
        activity.setWebsite(blankToNull(request.website()));
        activity.setTelegram(blankToNull(request.telegram()));
        activity.setWhatsapp(blankToNull(request.whatsapp()));
        activity.setFacebookUrl(blankToNull(request.facebookUrl()));
        activity.setCoverKey(blankToNull(request.coverKey()));
        if (request.categoryIds() != null || activity.getId() == null) {
            activity.setCategories(new HashSet<>(categoryService.getEntities(nullToEmpty(request.categoryIds()))));
        }
        if (request.tagIds() != null || activity.getId() == null) {
            activity.setTags(new HashSet<>(tagService.getEntities(nullToEmpty(request.tagIds()))));
        }
        if (request.interestIds() != null || activity.getId() == null) {
            activity.setInterests(new HashSet<>(interestService.getEntities(nullToEmpty(request.interestIds()))));
        }
        if (request.tourismRegionIds() != null || activity.getId() == null) {
            activity.setTourismRegions(new HashSet<>(regionService.getTourismEntities(nullToEmpty(request.tourismRegionIds()))));
        }
    }

    private void setStatus(Activity activity, ListingStatus status) {
        if (status == ListingStatus.PUBLISHED && activity.getStatus() != ListingStatus.PUBLISHED) {
            activity.setPublishedAt(Instant.now());
        }
        activity.setStatus(status);
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This activity is live: an editor makes changes to it now");
        }
    }

    private void checkWrite(UUID ownerUserId) {
        if (!permission.canWrite(ownerUserId, Authorities.current())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ownerUserId == null
                    ? "Name the account this activity belongs to"
                    : "This activity is listed in someone else's name");
        }
    }

    private ListingStatus permittedStatus(ListingStatus requested, ListingStatus current) {
        if (requested == null || requested == current) {
            return current;
        }
        if ((requested == ListingStatus.PUBLISHED || current == ListingStatus.PUBLISHED)
                && !permission.isEditor(Authorities.current())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only an editor can publish or unpublish an activity");
        }
        return requested;
    }

    private String slugFor(String wanted, String name, UUID id) {
        if (wanted != null && !wanted.isBlank()) {
            boolean taken = id == null ? activityRepository.existsBySlug(wanted) : activityRepository.existsBySlugAndIdNot(wanted, id);
            if (taken) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Another activity already uses the slug " + wanted);
            }
            return wanted;
        }
        return Slugs.unique(Slugs.of(name), activityRepository::existsBySlug);
    }

    private Activity find(UUID id) {
        return activityRepository.findWithLinksById(id).orElseThrow(ActivityNotFoundException::new);
    }

    private static <T> Set<T> nullToEmpty(Set<T> values) {
        return values == null ? Set.of() : values;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
