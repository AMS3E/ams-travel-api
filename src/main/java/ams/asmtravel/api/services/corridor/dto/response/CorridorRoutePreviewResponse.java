package ams.asmtravel.api.services.corridor.dto.response;

import ams.asmtravel.api.services.corridor.domain.CorridorTransportType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Roads between unsaved stops.
 *
 * @param configured false means every leg is empty
 * @param legs       one per stop after the first
 * @param error      why some legs are empty, or null
 */
public record CorridorRoutePreviewResponse(boolean configured, List<Leg> legs, String error) {

    /** One leg; figures are null when it could not be routed. */
    public record Leg(
            UUID fromDestinationId,
            UUID toDestinationId,
            CorridorTransportType transportType,
            BigDecimal distanceKm,
            Integer minutes,
            String polyline
    ) {
    }
}
