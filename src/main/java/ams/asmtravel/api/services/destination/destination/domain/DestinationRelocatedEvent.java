package ams.asmtravel.api.services.destination.destination.domain;

import java.util.UUID;

/** Sent when a destination's coordinates change, so routes through it are redrawn after commit. */
public record DestinationRelocatedEvent(UUID destinationId) {
}
