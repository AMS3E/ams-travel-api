package ams.asmtravel.api.services.history.architecture.domain;

import java.util.UUID;

/** A style is about to be deleted: the places built in it lose the tie. */
public record ArchitectureStyleDeletingEvent(UUID styleId) {
}
