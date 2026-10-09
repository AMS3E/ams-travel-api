package ams.asmtravel.api.services.verification.dto.response;

import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.publishing.service.PublishingService.Desk;
import ams.asmtravel.api.services.verification.domain.VerificationBasis;

import java.math.BigDecimal;

import java.time.Instant;
import java.util.UUID;

/**
 * A verification decision, used for history, queue and public status. In a
 * history row {@code verified} and {@code basis} are the decision's own; in a
 * queue row and the public status they are the place's standing now: a desk's
 * latest decision, or the travellers' seal when their reviews have earned it.
 *
 * @param id                  null for a place never decided on
 * @param desk                the place's desk; queue only
 * @param basis               what the verification rests on; TRAVELER_SCORE when only the reviews verify it
 * @param travelerRecommended whether the reviews have earned the Recommended by Traveler seal; null in a history row
 * @param ratingCount         star ratings so far; null in a history row
 * @param averageRating       their average; null when unrated or in a history row
 * @param evidenceUrl         readable only by verifying desks
 * @param createdAt           "verified since"
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
        VerificationBasis basis,
        Boolean travelerRecommended,
        Long ratingCount,
        BigDecimal averageRating,
        String reference,
        String note,
        String evidenceKey,
        String evidenceUrl,
        UUID decidedBy,
        String decidedByName,
        Instant createdAt
) {
}
