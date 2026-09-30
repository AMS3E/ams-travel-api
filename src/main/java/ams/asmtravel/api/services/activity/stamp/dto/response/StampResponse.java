package ams.asmtravel.api.services.activity.stamp.dto.response;

import ams.asmtravel.api.services.activity.stamp.domain.StampOutcome;
import ams.asmtravel.api.services.activity.stamp.domain.StampSource;
import ams.asmtravel.api.services.badge.dto.response.UserBadgeResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A collected stamp, or a check-in result. A listed stamp has no distance;
 * a check-in that collected nothing has no {@code id} or {@code collectedAt}.
 *
 * @param distanceMeters check-in only: how far the visitor was
 * @param radiusMeters   check-in only: how close they must be
 * @param badgesEarned   check-in only: badges this stamp reached, usually none
 */
public record StampResponse(
        UUID id,
        UUID destinationId,
        Instant collectedAt,
        StampSource source,
        StampOutcome outcome,
        Integer distanceMeters,
        Integer radiusMeters,
        List<UserBadgeResponse> badgesEarned
) {
}
