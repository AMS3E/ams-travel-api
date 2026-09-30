package ams.asmtravel.api.services.corridor.dto.response;

import ams.asmtravel.api.services.corridor.domain.CorridorRouteStatus;
import ams.asmtravel.api.services.corridor.domain.CorridorTransportType;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** A corridor with its days, stops and road route. */
public record CorridorItineraryResponse(
        CorridorResponse corridor,
        List<Day> days,
        Route route
) {

    /** @param distanceKm sum of the legs arriving at this day's stops */
    public record Day(
            UUID id,
            int dayNumber,
            String title,
            String description,
            LocalTime startTime,
            String notes,
            BigDecimal distanceKm,
            Integer travelMinutes,
            List<Stop> stops
    ) {
    }

    /**
     * @param sequence      number over the whole corridor, from 1 (the map marker)
     * @param stopOrder     order within the day, from 1
     * @param routePolyline road from the previous stop; null on the first stop
     */
    public record Stop(
            UUID id,
            int sequence,
            int stopOrder,
            Destination destination,
            LocalTime arrivalTime,
            LocalTime departureTime,
            Integer recommendedDurationMinutes,
            CorridorTransportType transportType,
            BigDecimal distanceFromPreviousKm,
            Integer travelTimeFromPreviousMinutes,
            String routePolyline,
            String title,
            String description,
            String notes
    ) {
    }

    /** The stop's place, read from the destination. */
    public record Destination(
            UUID id,
            String name,
            String slug,
            ListingStatus status,
            String shortDescription,
            String coverUrl,
            UUID provinceId,
            String provinceName,
            UUID regionId,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }

    /**
     * The road route over the whole corridor.
     *
     * @param encodedPolyline null until calculated
     * @param configured      whether a routing service is configured
     */
    public record Route(
            CorridorRouteStatus status,
            String encodedPolyline,
            BigDecimal totalDistanceKm,
            Integer totalDurationMinutes,
            Instant calculatedAt,
            String error,
            boolean configured
    ) {
    }
}
