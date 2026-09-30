package ams.asmtravel.api.services.user.user.service;

import ams.asmtravel.api.services.user.user.domain.User;
import ams.asmtravel.api.services.user.user.dto.request.IdentityRequest;
import ams.asmtravel.api.services.user.user.dto.response.IdentityResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Sign-in accounts (realm + account id) and customer-realm sign-ins; staff sign-ins belong to the staff service.
 * Keycloak methods do nothing when Keycloak admin is off (tests, catalogue-only).
 */
public interface IdentityService {

    /** The accounts on record, newest first; one person's when the request names them. */
    List<IdentityResponse> getAll(IdentityRequest request);

    /** The person behind a realm's account, if it has signed in before. */
    Optional<User> findUser(String provider, String providerUserId);

    /** Records that this account signs in as this person. Flushed, so a race shows here. */
    void link(User user, String provider, String providerUserId);

    /** The person's account id in a realm, if they have one there. */
    Optional<String> findProviderUserId(UUID userId, String provider);

    /**
     * Creates a customer sign-in (the email is the username); empty when Keycloak admin is off.
     *
     * @param password  the first password, or null to use "forgot password"
     * @param temporary true to make Keycloak ask for a new password at first sign-in
     */
    Optional<String> createAccount(String email, String name, String password, boolean temporary);

    /**
     * Syncs a customer sign-in with an edited profile (email, name, enabled); ignores unknown ids.
     *
     * @throws org.springframework.web.server.ResponseStatusException 409 when the email is taken
     * @throws org.springframework.web.server.ResponseStatusException 503 when Keycloak is down, so the edit rolls back
     */
    void updateAccount(String keycloakId, String email, String name, boolean enabled);

    /**
     * Deletes a customer sign-in; one already gone is not an error.
     *
     * @throws org.springframework.web.server.ResponseStatusException 503 when Keycloak is down, so the delete rolls back
     */
    void deleteAccount(String keycloakId);

    /** Removes a sign-in to undo a half-finished creation. Never throws. */
    void deleteAccountQuietly(String keycloakId);
}
