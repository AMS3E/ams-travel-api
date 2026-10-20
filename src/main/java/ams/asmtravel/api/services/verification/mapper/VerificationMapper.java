package ams.asmtravel.api.services.verification.mapper;

import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.publishing.service.PublishingService.Desk;
import ams.asmtravel.api.services.verification.domain.DestinationVerification;
import ams.asmtravel.api.services.verification.domain.Standing;
import ams.asmtravel.api.services.verification.domain.VerificationBasis;
import ams.asmtravel.api.services.verification.domain.VerifiedPlace;
import ams.asmtravel.api.services.verification.dto.response.VerificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Decisions and standings as the API answers them; the place itself comes from its own service. */
@Component
@RequiredArgsConstructor
public class VerificationMapper {

    private final MediaService mediaService;

    /**
     * @param desk          the place's desk for the queue; null in a history
     * @param decidedByName null if the decider no longer exists
     */
    public VerificationResponse toResponse(DestinationVerification decision, VerifiedPlace place, Desk desk, String decidedByName) {
        return new VerificationResponse(decision.getId(), place.type(), place.id(), place.name(), place.slug(),
                mediaService.url(place.coverKey()), place.status(), desk,
                Boolean.TRUE.equals(decision.getVerified()), decision.getBasis(), null, null, null,
                decision.getReference(), decision.getNote(), decision.getEvidenceKey(), mediaService.url(decision.getEvidenceKey()),
                decision.getDecidedBy(), decidedByName, decision.getCreatedAt());
    }

    /** Queue row for a place with no decision yet. */
    public VerificationResponse undecided(VerifiedPlace place, Desk desk) {
        return new VerificationResponse(null, place.type(), place.id(), place.name(), place.slug(), mediaService.url(place.coverKey()),
                place.status(), desk, false, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * A queue row as the place stands now: the latest decision, and on top of
     * it the travellers' seal, which verifies the place on its own.
     */
    public VerificationResponse standing(VerificationResponse latest, Standing standing) {
        boolean decided = latest.verified();
        return new VerificationResponse(latest.id(), latest.targetType(), latest.destinationId(), latest.destinationName(),
                latest.destinationSlug(), latest.destinationCoverUrl(), latest.destinationStatus(), latest.desk(),
                decided || standing.travelerRecommended(), basisOf(decided, latest.basis(), standing),
                standing.travelerRecommended(), standing.ratingCount(), standing.averageRating(),
                latest.reference(), latest.note(), latest.evidenceKey(), latest.evidenceUrl(),
                latest.decidedBy(), latest.decidedByName(), latest.createdAt());
    }

    /** Public status only: verified, on what, the travellers' seal, and since when. */
    public VerificationResponse publicStatus(UUID destinationId, DestinationVerification latest, Standing standing) {
        boolean decided = latest != null && Boolean.TRUE.equals(latest.getVerified());
        return new VerificationResponse(null, latest == null ? null : latest.getTargetType(), destinationId, null, null, null, null, null,
                decided || standing.travelerRecommended(), basisOf(decided, latest == null ? null : latest.getBasis(), standing),
                standing.travelerRecommended(), standing.ratingCount(), standing.averageRating(),
                null, null, null, null, null, null, latest == null ? null : latest.getCreatedAt());
    }

    /** A desk's basis while its decision stands; the travellers' when only their reviews verify the place. */
    private static VerificationBasis basisOf(boolean decided, VerificationBasis filed, Standing standing) {
        return decided ? filed : standing.travelerRecommended() ? VerificationBasis.TRAVELER_SCORE : null;
    }
}
