package ams.asmtravel.api.services.corridor.dto.response;

import java.util.UUID;

/** One stop in the flat list across all days; {@code sortOrder} counts from 1 over the whole corridor. */
public record CorridorStopResponse(
        UUID corridorId,
        UUID destinationId,
        Integer sortOrder
) {
}
