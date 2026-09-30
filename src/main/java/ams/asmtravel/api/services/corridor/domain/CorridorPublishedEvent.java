package ams.asmtravel.api.services.corridor.domain;

import java.util.UUID;

/** Sent when a corridor is published; after commit a missing route is calculated. */
public record CorridorPublishedEvent(UUID corridorId) {
}
