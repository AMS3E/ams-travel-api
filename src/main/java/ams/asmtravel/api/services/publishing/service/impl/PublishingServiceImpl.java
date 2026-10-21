package ams.asmtravel.api.services.publishing.service.impl;

import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.destination.activity.domain.Activity;
import ams.asmtravel.api.services.destination.activity.domain.ActivityDeletingEvent;
import ams.asmtravel.api.services.destination.activity.service.ActivityService;
import ams.asmtravel.api.services.destination.food.domain.Food;
import ams.asmtravel.api.services.destination.food.domain.FoodDeletingEvent;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.stay.domain.Stay;
import ams.asmtravel.api.services.destination.stay.domain.StayDeletingEvent;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.publishing.domain.PublishingRequest;
import ams.asmtravel.api.services.publishing.domain.PublishingStatus;
import ams.asmtravel.api.services.publishing.domain.PublishingTargetType;
import ams.asmtravel.api.services.publishing.dto.request.ApprovePublishRequest;
import ams.asmtravel.api.services.publishing.dto.request.RejectPublishRequest;
import ams.asmtravel.api.services.publishing.dto.request.SubmitPublishRequest;
import ams.asmtravel.api.services.publishing.dto.response.PublishingHistoryResponse;
import ams.asmtravel.api.services.publishing.dto.response.PublishingResponse;
import ams.asmtravel.api.services.publishing.exception.InvalidPublishingStateException;
import ams.asmtravel.api.services.publishing.exception.PublishingRequestNotFoundException;
import ams.asmtravel.api.services.publishing.mapper.PublishingMapper;
import ams.asmtravel.api.services.publishing.repository.PublishingRepository;
import ams.asmtravel.api.services.publishing.service.PublishingService;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import ams.asmtravel.api.services.user.user.domain.UserSummary;
import ams.asmtravel.api.services.user.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ams.asmtravel.api.services.authorization.domain.Permissions.DESTINATION_PUBLISH;
import static ams.asmtravel.api.services.authorization.domain.Permissions.DESTINATION_UNPUBLISH;
import static ams.asmtravel.api.services.authorization.domain.Permissions.FULL_CONTROL;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishingServiceImpl implements PublishingService {

    /** What a reviewer still has to act on. */
    private static final Set<PublishingStatus> PENDING = EnumSet.of(PublishingStatus.PENDING_REVIEW, PublishingStatus.APPROVED);

    private final PublishingRepository publishingRepository;
    private final DestinationService destinationService;
    private final StayService stayService;
    private final FoodService foodService;
    private final ActivityService activityService;
    private final UserService userService;
    private final PublishingMapper mapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional
    public PublishingResponse submit(SubmitPublishRequest submit, UUID requesterId) {
        Listing listing = listingOf(submit.targetType(), submit.targetId());
        if (!Authorities.has(Authorities.current(), FULL_CONTROL) && !listing.authors().contains(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only put forward listings you wrote");
        }
        if (listing.published()) {
            throw new InvalidPublishingStateException("That listing is already published");
        }
        Optional<PublishingRequest> latest = publishingRepository
                .findFirstByTargetTypeAndTargetIdOrderByRequestedAtDesc(submit.targetType(), submit.targetId());
        if (latest.map(request -> request.getStatus() == PublishingStatus.PENDING_REVIEW
                || request.getStatus() == PublishingStatus.APPROVED).orElse(false)) {
            throw new InvalidPublishingStateException("That listing already has a request under way");
        }
        // A request sent back to its author is submitted again; anything else starts a new one.
        PublishingRequest request = latest.filter(open -> open.getStatus() == PublishingStatus.DRAFT)
                .orElseGet(PublishingRequest::new);
        request.setTargetType(submit.targetType());
        request.setTargetId(submit.targetId());
        request.setTargetName(listing.name());
        request.setRequestedBy(requesterId);
        request.setStatus(PublishingStatus.PENDING_REVIEW);
        request.setRequestedAt(Instant.now());
        request = publishingRepository.saveAndFlush(request);
        log.info("Publishing request {} for {} {}", request.getId(), request.getTargetType(), request.getTargetId());
        return respond(request);
    }

    @Override
    @Transactional
    public PublishingResponse approve(UUID id, ApprovePublishRequest decision, UUID reviewerId) {
        PublishingRequest request = answerable(id, true);
        expect(request, PublishingStatus.PENDING_REVIEW, "Only a request waiting for review can be approved");
        return decide(request, PublishingStatus.APPROVED, decision == null ? null : decision.note(), reviewerId);
    }

    @Override
    @Transactional
    public PublishingResponse reject(UUID id, RejectPublishRequest decision, UUID reviewerId) {
        PublishingRequest request = answerable(id, false);
        expect(request, PublishingStatus.PENDING_REVIEW, "Only a request waiting for review can be rejected");
        PublishingStatus status = Boolean.TRUE.equals(decision.sendBack()) ? PublishingStatus.DRAFT : PublishingStatus.REJECTED;
        return decide(request, status, decision.note(), reviewerId);
    }

    @Override
    @Transactional
    public PublishingResponse publish(UUID id, UUID reviewerId) {
        PublishingRequest request = answerable(id, true);
        expect(request, PublishingStatus.APPROVED, "Only an approved request can be published");
        switch (request.getTargetType()) {
            case DESTINATION -> destinationService.publish(request.getTargetId());
            case STAY -> stayService.publish(request.getTargetId());
            case FOOD -> foodService.publish(request.getTargetId());
            case ACTIVITY -> activityService.publish(request.getTargetId());
        }
        request.setPublishedAt(Instant.now());
        return decide(request, PublishingStatus.PUBLISHED, request.getReviewNote(), reviewerId);
    }

    @Override
    @Transactional
    public PublishingResponse unpublish(UUID id, UUID reviewerId) {
        PublishingRequest request = find(id);
        if (!Authorities.hasAny(Authorities.current(), DESTINATION_UNPUBLISH, FULL_CONTROL)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account cannot take listings off the site");
        }
        expect(request, PublishingStatus.PUBLISHED, "Only a published request can be unpublished");
        switch (request.getTargetType()) {
            case DESTINATION -> destinationService.unpublish(request.getTargetId());
            case STAY -> stayService.unpublish(request.getTargetId());
            case FOOD -> foodService.unpublish(request.getTargetId());
            case ACTIVITY -> activityService.unpublish(request.getTargetId());
        }
        request.setUnpublishedAt(Instant.now());
        return decide(request, PublishingStatus.UNPUBLISHED, request.getReviewNote(), reviewerId);
    }

    @Override
    @Transactional(readOnly = true)
    public PublishingResponse findById(UUID id, UUID callerId) {
        PublishingRequest request = find(id);
        Map<UUID, UserSummary> people = people(Stream.of(request));
        if (!mayRead(request, people, callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "That request belongs to another desk");
        }
        return respond(request, people);
    }

    @Override
    @Transactional(readOnly = true)
    public PublishingResponse findCurrent(PublishingTargetType targetType, UUID targetId, UUID callerId) {
        PublishingRequest request = publishingRepository.findFirstByTargetTypeAndTargetIdOrderByRequestedAtDesc(targetType, targetId)
                .orElseThrow(PublishingRequestNotFoundException::new);
        return findById(request.getId(), callerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublishingHistoryResponse> findHistory(PublishingTargetType targetType, UUID targetId, UUID callerId) {
        List<PublishingRequest> requests = publishingRepository.findByTargetTypeAndTargetIdOrderByRequestedAtDesc(targetType, targetId);
        Map<UUID, UserSummary> people = people(requests.stream());
        if (!requests.stream().allMatch(request -> mayRead(request, people, callerId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "That listing's requests belong to another desk");
        }
        return requests.stream()
                .map(request -> mapper.toHistoryResponse(request, people.get(request.getRequestedBy()),
                        request.getReviewerId() == null ? null : people.get(request.getReviewerId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublishingResponse> findAll(PublishingStatus status) {
        return forMyDesks(status == null ? EnumSet.allOf(PublishingStatus.class) : EnumSet.of(status));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublishingResponse> findPending() {
        return forMyDesks(PENDING);
    }

    private List<PublishingResponse> forMyDesks(Set<PublishingStatus> statuses) {
        Set<Desk> desks = Desk.reviewedBy(Authorities.current());
        if (desks.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account reviews no publishing requests");
        }
        List<PublishingRequest> requests = publishingRepository.findByStatusInOrderByRequestedAtDesc(statuses);
        Map<UUID, UserSummary> people = people(requests.stream());
        return requests.stream()
                .filter(request -> desks.contains(deskOf(request, people)))
                .map(request -> respond(request, people))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublishingResponse> findByCreator(UUID creatorId, UUID callerId) {
        if (!creatorId.equals(callerId) && !Authorities.has(Authorities.current(), FULL_CONTROL)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only full control lists another person's requests");
        }
        List<PublishingRequest> requests = publishingRepository.findByRequestedByOrderByRequestedAtDesc(creatorId);
        Map<UUID, UserSummary> people = people(requests.stream());
        return requests.stream().map(request -> respond(request, people)).toList();
    }

    // A listing's requests go with it; a person with publishing history stays.

    @EventListener
    public void onDestinationDeleting(DestinationDeletingEvent event) {
        publishingRepository.deleteByTarget(PublishingTargetType.DESTINATION, event.destinationId());
    }

    @EventListener
    public void onStayDeleting(StayDeletingEvent event) {
        publishingRepository.deleteByTarget(PublishingTargetType.STAY, event.stayId());
    }

    @EventListener
    public void onFoodDeleting(FoodDeletingEvent event) {
        publishingRepository.deleteByTarget(PublishingTargetType.FOOD, event.foodId());
    }

    @EventListener
    public void onActivityDeleting(ActivityDeletingEvent event) {
        publishingRepository.deleteByTarget(PublishingTargetType.ACTIVITY, event.activityId());
    }

    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        if (publishingRepository.involves(event.userId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This person has publishing history, so they cannot be "
                    + "deleted. Set them to Inactive instead.");
        }
    }

    /** One listing put forward: its name, its authors, and whether it is live. */
    private record Listing(String name, Set<UUID> authors, boolean published) {
    }

    private Listing listingOf(PublishingTargetType type, UUID id) {
        return switch (type) {
            case DESTINATION -> {
                Destination destination = destinationService.getEntity(id);
                yield new Listing(destination.getName(), authors(destination.getCreatedBy()), destination.isPublished());
            }
            case STAY -> {
                Stay stay = stayService.getEntity(id);
                yield new Listing(stay.getName(), authors(stay.getOwnerUserId()), stay.getStatus() == ListingStatus.PUBLISHED);
            }
            case FOOD -> {
                Food food = foodService.getEntity(id);
                yield new Listing(food.getName(), authors(food.getOwnerUserId(), food.getCreatedBy()),
                        food.getStatus() == ListingStatus.PUBLISHED);
            }
            case ACTIVITY -> {
                Activity activity = activityService.getEntity(id);
                yield new Listing(activity.getName(), authors(activity.getOwnerUserId(), activity.getCreatedBy()),
                        activity.getStatus() == ListingStatus.PUBLISHED);
            }
        };
    }

    private static Set<UUID> authors(UUID... ids) {
        return Stream.of(ids).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }

    private PublishingResponse decide(PublishingRequest request, PublishingStatus status, String note, UUID reviewerId) {
        request.setStatus(status);
        request.setReviewNote(trimmedOrNull(note));
        request.setReviewerId(reviewerId);
        request.setReviewedAt(Instant.now());
        request = publishingRepository.saveAndFlush(request);
        log.info("Publishing request {} {}", request.getId(), status);
        events.publishEvent(new PublishingDecidedEvent(request.getId(), request.getTargetType(), request.getTargetId(),
                request.getTargetName(), request.getRequestedBy(), status, request.getReviewNote()));
        return respond(request);
    }

    private static void expect(PublishingRequest request, PublishingStatus status, String otherwise) {
        if (request.getStatus() != status) {
            throw new InvalidPublishingStateException(otherwise + "; this one is " + request.getStatus());
        }
    }

    /** @param ownCounts whether an editor or BDO may act on their own request (approve, publish) */
    private PublishingRequest answerable(UUID id, boolean ownCounts) {
        PublishingRequest request = find(id);
        Authentication caller = Authorities.current();
        boolean desk = Desk.reviewedBy(caller).contains(deskOf(request, people(Stream.of(request))));
        boolean own = ownCounts && Authorities.has(caller, DESTINATION_PUBLISH)
                && userService.findUserId(caller).map(me -> me.equals(request.getRequestedBy())).orElse(false);
        if (!desk && !own) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "That request belongs to another desk");
        }
        return request;
    }

    private boolean mayRead(PublishingRequest request, Map<UUID, UserSummary> people, UUID callerId) {
        return Objects.equals(request.getRequestedBy(), callerId)
                || Desk.reviewedBy(Authorities.current()).contains(deskOf(request, people));
    }

    private Desk deskOf(PublishingRequest request, Map<UUID, UserSummary> people) {
        UserSummary requester = people.get(request.getRequestedBy());
        return Desk.of(requester == null ? Set.of() : requester.roles());
    }

    private Map<UUID, UserSummary> people(Stream<PublishingRequest> requests) {
        return userService.findSummaries(requests
                .flatMap(request -> Stream.of(request.getRequestedBy(), request.getReviewerId()))
                .filter(Objects::nonNull)
                .toList());
    }

    private PublishingResponse respond(PublishingRequest request) {
        return respond(request, people(Stream.of(request)));
    }

    private PublishingResponse respond(PublishingRequest request, Map<UUID, UserSummary> people) {
        UserSummary requester = people.get(request.getRequestedBy());
        return mapper.toResponse(request, requester,
                request.getReviewerId() == null ? null : people.get(request.getReviewerId()),
                requester == null ? null : Desk.roleOf(requester.roles()),
                deskOf(request, people));
    }

    private PublishingRequest find(UUID id) {
        return publishingRepository.findById(id).orElseThrow(PublishingRequestNotFoundException::new);
    }

    private static String trimmedOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
