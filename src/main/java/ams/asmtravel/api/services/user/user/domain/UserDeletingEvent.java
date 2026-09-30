package ams.asmtravel.api.services.user.user.domain;

import java.util.UUID;

/**
 * Published in the delete transaction, before the person's row goes.
 * Listeners remove their own rows about the person, or throw to refuse the delete.
 */
public record UserDeletingEvent(UUID userId) {
}
