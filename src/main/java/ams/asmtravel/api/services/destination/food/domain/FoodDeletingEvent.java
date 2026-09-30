package ams.asmtravel.api.services.destination.food.domain;

import java.util.UUID;

/** Sent in the delete transaction, before the row goes, so its reviews and favorites go too. */
public record FoodDeletingEvent(UUID foodId) {
}
