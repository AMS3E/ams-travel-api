package ams.asmtravel.api.services.verification.service.impl;

import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.exception.DestinationNotFoundException;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.media.domain.MediaType;
import ams.asmtravel.api.services.media.service.MediaService;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Records verification decisions, checks access via {@link VerifierDesks} and what
 * each basis needs, and reads the travellers' standing from the review service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationServiceImpl implements VerificationService {

    private final DestinationVerificationRepository verificationRepository;
    private final DestinationService destinationService;
    private final UserService userService;
    private final MediaService mediaService;
    private final VerifierDesks verifierDesks;
    private final VerificationMapper mapper;
    private final ReviewService reviewService;
    private final VerificationProperties properties;

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> findQueue(Authentication authentication) {
        Set<Desk> desks = verifierDesks.desksOf(authentication);
        if (desks.isEmpty()) {
            return List.of();
        }
        List<DestinationSummary> places = destinationService.findAllSummaries();
        Map<UUID, Desk> deskOf = desksOf(places);
        List<DestinationSummary> reachable = places.stream().filter(place -> desks.contains(deskOf.get(place.id()))).toList();
        if (reachable.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = reachable.stream().map(DestinationSummary::id).toList();
        Map<UUID, DestinationVerification> latest = latestOf(ids);
        Map<UUID, Standing> standings = standingsOf(ids);
        Map<UUID, UserSummary> deciders = userService.findSummaries(
                latest.values().stream().map(DestinationVerification::getDecidedBy).toList());
        return reachable.stream()
                .map(place -> {
                    DestinationVerification last = latest.get(place.id());
                    VerificationResponse decision = last == null
                            ? mapper.undecided(place, deskOf.get(place.id()))
                            : mapper.toResponse(last, deskOf.get(place.id()), nameOf(deciders.get(last.getDecidedBy())));
                    return mapper.standing(decision, standings.getOrDefault(place.id(), Standing.NONE));
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> findHistory(UUID destinationId, Authentication authentication) {
        requireReach(destinationId, authentication);
        List<DestinationVerification> history = verificationRepository.findHistory(destinationId);
        Map<UUID, UserSummary> deciders = userService.findSummaries(
                history.stream().map(DestinationVerification::getDecidedBy).toList());
        return history.stream()
                .map(decision -> mapper.toResponse(decision, null, nameOf(deciders.get(decision.getDecidedBy()))))
                .toList();
    }

    @Override
    @Transactional
    public VerificationResponse record(VerificationRequest request, UUID deciderId, Authentication authentication) {
        UUID destinationId = request.destinationId();
        Desk desk = requireReach(destinationId, authentication);
        boolean verified = request.verified();
        VerificationBasis basis = verified ? request.basis() : null;
        String note = trimmedOrNull(request.note());
        String evidenceKey = trimmedOrNull(request.evidenceKey());
        String reference = trimmedOrNull(request.reference());

        if (verified && basis == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Say what the verification rests on: an official site, a partnership, or a payment");
        }
        if (verified && !basis.filed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Recommended by Traveler is earned from reviews; it cannot be filed");
        }
        // A paid or partner verification needs the paperwork, or at least its number.
        if (verified && basis.needsPaperwork() && reference == null && evidenceKey == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, basis == VerificationBasis.PAID
                    ? "Attach the invoice or receipt, or give its number, to verify"
                    : "Attach the partnership agreement, or give its reference, to verify");
        }
        if (verified && basis == VerificationBasis.PARTNER && desk != Desk.PARTNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only a partner's own listing is verified as a partner's; this one was written by "
                            + (desk == Desk.CONTRIBUTOR ? "a contributor" : "our team"));
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
        decision.setDestination(destinationService.getEntity(destinationId));
        decision.setVerified(verified);
        decision.setBasis(basis);
        decision.setReference(reference);
        decision.setNote(note);
        decision.setEvidenceKey(evidenceKey);
        decision.setDecidedBy(deciderId);
        decision = verificationRepository.saveAndFlush(decision);
        log.info("Destination {} {} by user {}", destinationId, verified ? "verified" : "unverified", deciderId);
        return mapper.toResponse(decision, null, nameOf(userService.findSummary(deciderId).orElse(null)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> findStatuses(List<UUID> destinationIds) {
        if (!destinationIds.isEmpty()) {
            List<UUID> ids = destinationIds.stream().distinct().toList();
            Map<UUID, DestinationVerification> latest = latestOf(ids);
            Map<UUID, Standing> standings = standingsOf(ids);
            return ids.stream()
                    .map(id -> mapper.publicStatus(id, latest.get(id), standings.getOrDefault(id, Standing.NONE)))
                    .toList();
        }
        // Every published place that is verified: by a desk's decision, or by its reviews.
        List<UUID> published = destinationService.findPublishedSummaries().stream().map(DestinationSummary::id).toList();
        Map<UUID, DestinationVerification> latest = new HashMap<>();
        verificationRepository.findAllNewestFirst().forEach(decision -> latest.putIfAbsent(decision.getDestination().getId(), decision));
        Map<UUID, Standing> standings = standingsOf(published);
        return published.stream()
                .map(id -> mapper.publicStatus(id, latest.get(id), standings.getOrDefault(id, Standing.NONE)))
                .filter(VerificationResponse::verified)
                .toList();
    }

    @EventListener
    public void onDestinationDeleting(DestinationDeletingEvent event) {
        verificationRepository.deleteByDestinationId(event.destinationId());
    }

    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        if (verificationRepository.existsByDecidedBy(event.userId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This person has made verification decisions, which have to outlive them");
        }
    }

    /** The place's desk; 404 if the place is missing, 403 if outside the caller's desks. */
    private Desk requireReach(UUID destinationId, Authentication authentication) {
        DestinationSummary place = destinationService.findSummary(destinationId)
                .orElseThrow(() -> new DestinationNotFoundException());
        Desk desk = desksOf(List.of(place)).get(place.id());
        if (!verifierDesks.desksOf(authentication).contains(desk)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your desk does not verify this destination");
        }
        return desk;
    }

    /** Each place's star ratings and whether they earn the travellers' seal; the review service answers 100 at a time. */
    private Map<UUID, Standing> standingsOf(List<UUID> destinationIds) {
        Map<UUID, Standing> standings = new HashMap<>();
        VerificationProperties.TravelerSeal seal = properties.travelerSeal();
        for (int from = 0; from < destinationIds.size(); from += 100) {
            List<UUID> chunk = destinationIds.subList(from, Math.min(from + 100, destinationIds.size()));
            for (ReviewSummaryResponse summary : reviewService.summaries(ReviewTargetType.DESTINATION, chunk)) {
                standings.put(summary.targetId(), new Standing(summary.ratingCount(), summary.averageRating(),
                        seal.earned(summary.ratingCount(), summary.averageRating())));
            }
        }
        return standings;
    }

    /** Each place's desk, from its author's roles. */
    private Map<UUID, Desk> desksOf(Collection<DestinationSummary> places) {
        Map<UUID, UserSummary> authors = userService.findSummaries(
                places.stream().map(DestinationSummary::createdBy).filter(Objects::nonNull).toList());
        Map<UUID, Desk> desks = new HashMap<>();
        for (DestinationSummary place : places) {
            UserSummary author = place.createdBy() == null ? null : authors.get(place.createdBy());
            desks.put(place.id(), verifierDesks.deskOf(author == null ? Set.of() : author.roles()));
        }
        return desks;
    }

    private Map<UUID, DestinationVerification> latestOf(Collection<UUID> destinationIds) {
        Map<UUID, DestinationVerification> latest = new HashMap<>();
        if (!destinationIds.isEmpty()) {
            verificationRepository.findNewestFirst(Set.copyOf(destinationIds))
                    .forEach(decision -> latest.putIfAbsent(decision.getDestination().getId(), decision));
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
