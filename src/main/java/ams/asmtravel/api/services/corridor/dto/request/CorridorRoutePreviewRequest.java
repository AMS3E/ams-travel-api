package ams.asmtravel.api.services.corridor.dto.request;

import ams.asmtravel.api.services.corridor.domain.CorridorTransportType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Unsaved stops, in order, for the CMS planning map. Nothing is stored.
 *
 * @param stops at least two; the first stop's transport is ignored
 */
public record CorridorRoutePreviewRequest(@NotNull @Size(min = 2, max = 150) List<@NotNull @Valid Stop> stops) {

    /** @param transportType null means by car */
    public record Stop(@NotNull UUID destinationId, CorridorTransportType transportType) {
    }
}
