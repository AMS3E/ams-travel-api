package ams.asmtravel.api.services.verification.dto.response;

import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.publishing.service.PublishingService.Desk;

import java.time.Instant;
import java.util.UUID;

/**
 * A verification decision, used for history, queue and public status (public keeps only id, verified, date).
 *
 * @param id          null for a place never decided on
 * @param desk        the place's desk; queue only
 * @param evidenceUrl readable only by verifying desks
 * @param createdAt   "verified since"
 */
public record VerificationResponse(
        UUID id,
        UUID destinationId,
        String destinationName,
        String destinationSlug,
        String destinationCoverUrl,
        ListingStatus destinationStatus,
        Desk desk,
        boolean verified,
        String reference,
        String note,
        String evidenceKey,
        String evidenceUrl,
        UUID decidedBy,
        String decidedByName,
        Instant createdAt
) {
}
