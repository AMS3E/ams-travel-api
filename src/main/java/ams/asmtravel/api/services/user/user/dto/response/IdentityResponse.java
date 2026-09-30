package ams.asmtravel.api.services.user.user.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * One account a person signs in with.
 *
 * @param provider       the Keycloak realm: {@code amstravel} for customers, {@code amsadmin} for staff
 * @param providerUserId the account's id in that realm
 * @param createdAt      when it first signed in here
 */
public record IdentityResponse(
        UUID id,
        UUID userId,
        String provider,
        String providerUserId,
        Instant createdAt
) {
}
