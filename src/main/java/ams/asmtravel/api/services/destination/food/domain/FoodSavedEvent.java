package ams.asmtravel.api.services.destination.food.domain;

import java.util.UUID;

/** Sent after a food listing is saved, so the menu resets its "from" price. */
public record FoodSavedEvent(UUID foodId) {
}
