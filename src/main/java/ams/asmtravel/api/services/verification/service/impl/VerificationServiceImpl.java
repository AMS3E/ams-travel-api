package ams.asmtravel.api.services.verification.service.impl;

import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.exception.DestinationNotFoundException;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.media.domain.MediaType;
import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.publishing.service.PublishingService.Desk;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import ams.asmtravel.api.services.user.user.domain.UserSummary;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.verification.domain.DestinationVerification;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Records verification decisions; checks access via {@link VerifierDesks} and the required evidence or reason. */
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
        Map<UUID, DestinationVerification> latest = latestOf(reachable.stream().map(DestinationSummary::id).toList());
        Map<UUID, UserSummary> deciders = userService.findSummaries(
                latest.values().stream().map(DestinationVerification::getDecidedBy).toList());
        return reachable.stream()
                .map(place -> {
                    DestinationVerification last = latest.get(place.id());
                    return last == null
                            ? mapper.undecided(place, deskOf.get(place.id()))
                            : mapper.toResponse(last, deskOf.get(place.id()), nameOf(deciders.get(last.getDecidedBy())));
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
        requireReach(destinationId, authentication);
        boolean verified = request.verified();
        String note = trimmedOrNull(request.note());
        String evidenceKey = trimmedOrNull(request.evidenceKey());
        String reference = trimmedOrNull(request.reference());

        // Verifying needs the evidence file or at least its number.
        if (verified && reference == null && evidenceKey == null) {
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
        decision.setDestination(destinationService.getEntity(destinationId));
        decision.setVerified(verified);
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
            Map<UUID, DestinationVerification> latest = latestOf(destinationIds);
            return destinationIds.stream().distinct().map(id -> mapper.publicStatus(id, latest.get(id))).toList();
        }
        Set<UUID> published = new HashSet<>();
        destinationService.findPublishedSummaries().forEach(place -> published.add(place.id()));
        Map<UUID, DestinationVerification> latest = new HashMap<>();
        verificationRepository.findAllNewestFirst().forEach(decision -> latest.putIfAbsent(decision.getDestination().getId(), decision));
        return latest.entrySet().stream()
                .filter(entry -> published.contains(entry.getKey()) && entry.getValue().getVerified())
                .map(entry -> mapper.publicStatus(entry.getKey(), entry.getValue()))
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

    /** 404 if the place is missing; 403 if outside the caller's desks. */
    private void requireReach(UUID destinationId, Authentication authentication) {
        DestinationSummary place = destinationService.findSummary(destinationId)
                .orElseThrow(() -> new DestinationNotFoundException());
        Desk desk = desksOf(List.of(place)).get(place.id());
        if (!verifierDesks.desksOf(authentication).contains(desk)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your desk does not verify this destination");
        }
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
