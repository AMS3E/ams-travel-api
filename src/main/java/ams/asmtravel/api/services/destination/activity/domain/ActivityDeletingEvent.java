package ams.asmtravel.api.services.destination.activity.domain;

import java.util.UUID;

/** Sent in the delete transaction, before the row goes, so its reviews, photos and favorites go too. */
public record ActivityDeletingEvent(UUID activityId) {
}
