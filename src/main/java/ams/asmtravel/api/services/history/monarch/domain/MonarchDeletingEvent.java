package ams.asmtravel.api.services.history.monarch.domain;

import java.util.UUID;

/** A monarch is about to be deleted: the places tied to them lose the tie. */
public record MonarchDeletingEvent(UUID monarchId) {
}
