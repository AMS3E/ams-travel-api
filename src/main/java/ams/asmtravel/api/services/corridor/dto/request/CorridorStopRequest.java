package ams.asmtravel.api.services.corridor.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** Ordered destinations; replaces all stops with a one-day itinerary. Multi-day corridors use the itinerary. */
public record CorridorStopRequest(@NotNull List<@NotNull UUID> destinationIds) {
}
