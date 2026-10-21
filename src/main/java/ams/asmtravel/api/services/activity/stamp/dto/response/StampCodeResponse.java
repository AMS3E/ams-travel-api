package ams.asmtravel.api.services.activity.stamp.dto.response;

import ams.asmtravel.api.services.activity.stamp.domain.StampTarget;

import java.time.Instant;
import java.util.UUID;

/**
 * One of a destination's check-in codes: a print posted somewhere at the place.
 *
 * @param code      what is printed under the QR, for a visitor to type
 * @param label     where this print is posted; null when not said
 * @param payload   what the QR encodes: a link to the scan page, whose last part is the code
 * @param checkIns  how many check-ins were collected with this code
 * @param retiredAt null while the code works; a retired code scans as invalid
 */
public record StampCodeResponse(
        UUID id,
        StampTarget targetType,
        UUID targetId,
        String code,
        String label,
        String payload,
        Instant issuedAt,
        UUID issuedBy,
        String issuedByName,
        long checkIns,
        Instant retiredAt,
        UUID retiredBy,
        String retiredByName
) {
}
