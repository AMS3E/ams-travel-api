package ams.asmtravel.api.services.corridor.domain;

import java.util.UUID;

/** Sent when a corridor's days or stops change; after commit the route is recalculated if stale. */
public record CorridorItineraryChangedEvent(UUID corridorId) {
}
