package ams.asmtravel.api.services.verification.service.impl;

import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.exception.DestinationNotFoundException;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.destination.activity.domain.ActivityDeletingEvent;
import ams.asmtravel.api.services.destination.activity.service.ActivityService;
import ams.asmtravel.api.services.destination.food.domain.FoodDeletingEvent;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.stay.domain.StayDeletingEvent;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.media.domain.MediaType;
import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.publishing.domain.PublishingTargetType;
import ams.asmtravel.api.services.publishing.service.PublishingService.Desk;
import ams.asmtravel.api.services.review.review.domain.ReviewTargetType;
import ams.asmtravel.api.services.review.review.dto.response.ReviewSummaryResponse;
import ams.asmtravel.api.services.review.review.service.ReviewService;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import ams.asmtravel.api.services.user.user.domain.UserSummary;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.verification.config.VerificationProperties;
import ams.asmtravel.api.services.verification.domain.DestinationVerification;
import ams.asmtravel.api.services.verification.domain.Standing;
import ams.asmtravel.api.services.verification.domain.VerificationBasis;
import ams.asmtravel.api.services.verification.domain.VerifiedPlace;
import ams.asmtravel.api.services.verification.dto.request.VerificationRequest;
import ams.asmtravel.api.services.verification.dto.response.VerificationResponse;
import ams.asmtravel.api.services.verification.mapper.VerificationMapper;
import ams.asmtravel.api.services.verification.repository.DestinationVerificationRepository;
import ams.asmtravel.api.services.verification.security.VerifierDesks;
import ams.asmtravel.api.services.verification.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Records verification decisions on places of any kind, checks access via {@link VerifierDesks} and what
 * each basis needs, and reads the travellers' standing from the review service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {

    private static final int SUMMARY_BATCH = 100;

    private final DestinationVerificationRepository verificationRepository;
    private final DestinationService destinationService;
    private final StayService stayService;
    private final FoodService foodService;
    private final ActivityService activityService;
    private final UserService userService;
    private final MediaService mediaService;
    private final VerifierDesks verifierDesks;
    private final VerificationMapper mapper;
    private final ReviewService reviewService;
    private final VerificationProperties properties;

    /** A place the caller may reach, and the desk it is on. */
    private record Reach(VerifiedPlace place, Desk desk) {
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> findQueue(Authentication authentication) {
        Set<Desk> desks = verifierDesks.desksOf(authentication);
        if (desks.isEmpty()) {
            return List.of();
        }
        List<VerifiedPlace> places = allPlaces();
        Map<UUID, Desk> deskOf = desksOf(places);
        List<VerifiedPlace> reachable = places.stream().filter(place -> desks.contains(deskOf.get(place.id()))).toList();
        if (reachable.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = reachable.stream().map(VerifiedPlace::id).toList();
        Map<UUID, DestinationVerification> latest = latestOf(ids);
        Map<UUID, Standing> standings = standingsOf(reachable);
        Map<UUID, UserSummary> deciders = userService.findSummaries(
                latest.values().stream().map(DestinationVerification::getDecidedBy).toList());
        return reachable.stream()
                .map(place -> {
                    DestinationVerification last = latest.get(place.id());
                    VerificationResponse decision = last == null
                            ? mapper.undecided(place, deskOf.get(place.id()))
                            : mapper.toResponse(last, place, deskOf.get(place.id()), nameOf(deciders.get(last.getDecidedBy())));
                    return mapper.standing(decision, standings.getOrDefault(place.id(), Standing.NONE));
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> findHistory(UUID destinationId, Authentication authentication) {
        Reach reach = requireReach(destinationId, authentication);
        List<DestinationVerification> history = verificationRepository.findByTargetIdOrderByCreatedAtDescIdDesc(destinationId);
        Map<UUID, UserSummary> deciders = userService.findSummaries(
                history.stream().map(DestinationVerification::getDecidedBy).toList());
        return history.stream()
                .map(decision -> mapper.toResponse(decision, reach.place(), null, nameOf(deciders.get(decision.getDecidedBy()))))
                .toList();
    }

    @Override
    @Transactional
    public VerificationResponse record(VerificationRequest request, UUID deciderId, Authentication authentication) {
        UUID destinationId = request.destinationId();
        Reach reach = requireReach(destinationId, authentication);
        boolean verified = request.verified();
        VerificationBasis basis = verified ? request.basis() : null;
        String note = trimmedOrNull(request.note());
        String evidenceKey = trimmedOrNull(request.evidenceKey());
        String reference = trimmedOrNull(request.reference());

        if (verified && basis == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Say what the verification rests on: a payment to AMS Travel");
        }
        if (verified && !basis.filed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The traveller score is earned from reviews; it cannot be filed");
        }
        // A paid verification needs the paperwork, or at least its number.
        if (verified && basis.needsPaperwork() && reference == null && evidenceKey == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Attach the invoice or receipt, or give its number, to verify");
        }
        DestinationVerification last = latestOf(List.of(destinationId)).get(destinationId);
        if (!verified && (last == null || !last.getVerified())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That destination is not verified");
        }
        if (!verified && note == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Say why the verification is being revoked");
        }
        if (evidenceKey != null && !mediaService.isStored(evidenceKey, MediaType.VERIFICATION)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Upload the evidence through the verification form before attaching it");
        }

        DestinationVerification decision = new DestinationVerification();
        decision.setTargetType(reach.place().type());
        decision.setTargetId(reach.place().id());
        decision.setVerified(verified);
        decision.setBasis(basis);
        decision.setReference(reference);
        decision.setNote(note);
        decision.setEvidenceKey(evidenceKey);
        decision.setDecidedBy(deciderId);
        decision = verificationRepository.saveAndFlush(decision);
        log.info("{} {} {} by user {}", reach.place().type(), destinationId, verified ? "verified" : "unverified", deciderId);
        return mapper.toResponse(decision, reach.place(), null, nameOf(userService.findSummary(deciderId).orElse(null)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> findStatuses(List<UUID> destinationIds) {
        if (!destinationIds.isEmpty()) {
            List<UUID> ids = destinationIds.stream().distinct().toList();
            Map<UUID, DestinationVerification> latest = latestOf(ids);
            Map<UUID, Standing> standings = standingsOfAnyKind(ids);
            return ids.stream()
                    .map(id -> mapper.publicStatus(id, latest.get(id), standings.getOrDefault(id, Standing.NONE)))
                    .toList();
        }
        // Every published place of any kind that is verified: by a desk's decision, or by its reviews.
        List<VerifiedPlace> published = publishedPlaces();
        Map<UUID, DestinationVerification> latest = new HashMap<>();
        verificationRepository.findAllByOrderByCreatedAtDescIdDesc().forEach(decision -> latest.putIfAbsent(decision.getTargetId(), decision));
        Map<UUID, Standing> standings = standingsOf(published);
        return published.stream()
                .map(place -> mapper.publicStatus(place.id(), latest.get(place.id()), standings.getOrDefault(place.id(), Standing.NONE)))
                .filter(VerificationResponse::verified)
                .toList();
    }

    // A place's decisions go with it; a decider's outlive them.

    @EventListener
    public void onDestinationDeleting(DestinationDeletingEvent event) {
        verificationRepository.deleteByTargetId(event.destinationId());
    }

    @EventListener
    public void onStayDeleting(StayDeletingEvent event) {
        verificationRepository.deleteByTargetId(event.stayId());
    }

    @EventListener
    public void onFoodDeleting(FoodDeletingEvent event) {
        verificationRepository.deleteByTargetId(event.foodId());
    }

    @EventListener
    public void onActivityDeleting(ActivityDeletingEvent event) {
        verificationRepository.deleteByTargetId(event.activityId());
    }

    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        if (verificationRepository.existsByDecidedBy(event.userId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This person has made verification decisions, which have to outlive them");
        }
    }

    // Helpers

    /** The place and its desk; 404 if no place of any kind has the id, 403 if outside the caller's desks. */
    private Reach requireReach(UUID destinationId, Authentication authentication) {
        VerifiedPlace place = find(destinationId).orElseThrow(() -> new DestinationNotFoundException());
        Desk desk = desksOf(List.of(place)).get(place.id());
        if (!verifierDesks.desksOf(authentication).contains(desk)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your desk does not verify this destination");
        }
        return new Reach(place, desk);
    }

    /** A place of any kind by its id: attractions first, as most are. */
    private Optional<VerifiedPlace> find(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        Optional<VerifiedPlace> attraction = destinationService.findSummary(id).map(VerifiedPlace::of);
        if (attraction.isPresent()) {
            return attraction;
        }
        if (stayService.exists(id)) {
            return Optional.of(VerifiedPlace.of(stayService.findById(id)));
        }
        if (foodService.exists(id)) {
            return Optional.of(VerifiedPlace.of(foodService.findById(id)));
        }
        if (activityService.exists(id)) {
            return Optional.of(VerifiedPlace.of(activityService.findById(id)));
        }
        return Optional.empty();
    }

    /** Every place of every kind in any status, for the queue. */
    private List<VerifiedPlace> allPlaces() {
        List<VerifiedPlace> places = new ArrayList<>();
        destinationService.findAllSummaries().forEach(place -> places.add(VerifiedPlace.of(place)));
        stayService.getAll(null).forEach(stay -> places.add(VerifiedPlace.of(stay)));
        foodService.getAll(null, null).forEach(food -> places.add(VerifiedPlace.of(food)));
        activityService.getAll(null, null).forEach(activity -> places.add(VerifiedPlace.of(activity)));
        return places;
    }

    /** Every published place of every kind, for the public list of verified places. */
    private List<VerifiedPlace> publishedPlaces() {
        List<VerifiedPlace> places = new ArrayList<>();
        destinationService.findPublishedSummaries().forEach(place -> places.add(VerifiedPlace.of(place)));
        stayService.getAll(null).stream().map(VerifiedPlace::of).filter(VerifiedPlace::published).forEach(places::add);
        foodService.getAll(null, null).stream().map(VerifiedPlace::of).filter(VerifiedPlace::published).forEach(places::add);
        activityService.getAll(null, null).stream().map(VerifiedPlace::of).filter(VerifiedPlace::published).forEach(places::add);
        return places;
    }

    /** Each place's star ratings and whether they verify it on their own, read per kind; the review service answers 100 at a time. */
    private Map<UUID, Standing> standingsOf(Collection<VerifiedPlace> places) {
        Map<PublishingTargetType, List<UUID>> byKind = new EnumMap<>(PublishingTargetType.class);
        for (VerifiedPlace place : places) {
            byKind.computeIfAbsent(place.type(), kind -> new ArrayList<>()).add(place.id());
        }
        Map<UUID, Standing> standings = new HashMap<>();
        byKind.forEach((kind, ids) -> standings.putAll(standingsOf(kind, ids)));
        return standings;
    }

    /**
     * The same for ids whose kind is not known: each kind's reviews are asked, and the answer with ratings
     * wins, since a kind that has none for an id answers zero rather than nothing.
     */
    private Map<UUID, Standing> standingsOfAnyKind(List<UUID> ids) {
        Map<UUID, Standing> standings = new HashMap<>();
        for (PublishingTargetType kind : PublishingTargetType.values()) {
            standingsOf(kind, ids).forEach((id, standing) ->
                    standings.merge(id, standing, (kept, other) -> other.ratingCount() > kept.ratingCount() ? other : kept));
        }
        return standings;
    }

    private Map<UUID, Standing> standingsOf(PublishingTargetType kind, List<UUID> ids) {
        Map<UUID, Standing> standings = new HashMap<>();
        VerificationProperties.TravelerScore score = properties.travelerScore();
        for (int from = 0; from < ids.size(); from += SUMMARY_BATCH) {
            List<UUID> chunk = ids.subList(from, Math.min(from + SUMMARY_BATCH, ids.size()));
            for (ReviewSummaryResponse summary : reviewService.summaries(reviewTarget(kind), chunk)) {
                standings.put(summary.targetId(), new Standing(summary.ratingCount(), summary.averageRating(),
                        score.earned(summary.ratingCount(), summary.averageRating())));
            }
        }
        return standings;
    }

    private static ReviewTargetType reviewTarget(PublishingTargetType kind) {
        return switch (kind) {
            case DESTINATION -> ReviewTargetType.DESTINATION;
            case STAY -> ReviewTargetType.STAY;
            case FOOD -> ReviewTargetType.FOOD;
            case ACTIVITY -> ReviewTargetType.ACTIVITY;
        };
    }

    /** Each place's desk, from its author's roles. */
    private Map<UUID, Desk> desksOf(Collection<VerifiedPlace> places) {
        Map<UUID, UserSummary> authors = userService.findSummaries(
                places.stream().map(VerifiedPlace::authorId).filter(Objects::nonNull).distinct().toList());
        Map<UUID, Desk> desks = new HashMap<>();
        for (VerifiedPlace place : places) {
            UserSummary author = place.authorId() == null ? null : authors.get(place.authorId());
            desks.put(place.id(), verifierDesks.deskOf(author == null ? Set.of() : author.roles()));
        }
        return desks;
    }

    private Map<UUID, DestinationVerification> latestOf(Collection<UUID> destinationIds) {
        Map<UUID, DestinationVerification> latest = new HashMap<>();
        if (!destinationIds.isEmpty()) {
            verificationRepository.findByTargetIdInOrderByCreatedAtDescIdDesc(Set.copyOf(destinationIds))
                    .forEach(decision -> latest.putIfAbsent(decision.getTargetId(), decision));
        }
        return latest;
    }

    private static String nameOf(UserSummary person) {
        return person == null ? null : person.fullName();
    }

    private static String trimmedOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
