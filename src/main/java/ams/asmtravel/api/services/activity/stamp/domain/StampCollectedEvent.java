package ams.asmtravel.api.services.activity.stamp.domain;

import java.util.UUID;

/** A stamp was just collected; sent inside its transaction so badge counts include it. */
public record StampCollectedEvent(UUID userId, UUID destinationId, UUID stampId) {
}
