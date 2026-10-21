package ams.asmtravel.api.services.activity.stamp.dto.response;

import ams.asmtravel.api.services.activity.stamp.domain.StampTarget;

import java.time.Instant;
import java.util.UUID;

/**
 * A destination's check-in code.
 *
 * @param code       what is printed under the QR, for a visitor to type
 * @param payload    what the QR encodes: a link to the scan page, whose last part is the code
 * @param replacedAt null while current
 */
public record StampCodeResponse(
        UUID id,
        StampTarget targetType,
        UUID targetId,
        String code,
        String payload,
        Instant issuedAt,
        UUID issuedBy,
        Instant replacedAt
) {
}
