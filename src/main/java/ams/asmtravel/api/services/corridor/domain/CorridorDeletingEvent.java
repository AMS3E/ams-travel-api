package ams.asmtravel.api.services.corridor.domain;

import java.util.UUID;

/** Sent inside the delete transaction, before the row goes, so favorites can be removed. */
public record CorridorDeletingEvent(UUID corridorId) {
}
