package ams.asmtravel.api.services.destination.stay.domain;

import java.util.UUID;

/** Sent after a stay is saved, so its rooms reset the "from" price. */
public record StaySavedEvent(UUID stayId) {
}
