package ams.asmtravel.api.services.history.religion.domain;

import java.util.UUID;

/** A religion is about to be deleted: the places tied to it lose the tie. */
public record ReligionDeletingEvent(UUID religionId) {
}
