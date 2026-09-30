package ams.asmtravel.api.services.corridor.dto.request;

import ams.asmtravel.api.services.corridor.domain.CorridorTransportType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** The whole itinerary; replaces all days and stops. List order sets day numbers and stop order. */
public record CorridorItineraryRequest(@NotNull @Size(max = 30) List<@NotNull @Valid Day> days) {

    public record Day(
            @Size(max = 200) String title,
            String description,
            LocalTime startTime,
            @Size(max = 4000) String notes,
            @NotNull @Size(max = 40) List<@NotNull @Valid Stop> stops
    ) {
    }

    /** @param transportType from the previous stop; ignored on the first stop */
    public record Stop(
            @NotNull UUID destinationId,
            LocalTime arrivalTime,
            LocalTime departureTime,
            @Min(0) @Max(1440) Integer recommendedDurationMinutes,
            CorridorTransportType transportType,
            @Size(max = 200) String title,
            String description,
            @Size(max = 4000) String notes
    ) {
    }
}
