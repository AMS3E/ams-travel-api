package ams.asmtravel.api.services.destination.destination.domain;

import java.util.UUID;

/**
 * Sent in the delete transaction, before the row goes, so other services remove their own rows.
 * Browse events and stamps are kept so badge counts survive.
 */
public record DestinationDeletingEvent(UUID destinationId) {
}
