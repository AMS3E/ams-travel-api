package ams.asmtravel.api.services.user.user.dto.request;

import java.util.UUID;

/**
 * Filter for {@code GET /api/v1/identities}; every field is optional.
 *
 * @param userId one account's identities, or null for all
 */
public record IdentityRequest(UUID userId) {
}
