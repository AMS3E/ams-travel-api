package ams.asmtravel.api.services.user.user.service;

import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.user.user.domain.User;
import ams.asmtravel.api.services.user.user.domain.UserSummary;
import ams.asmtravel.api.services.user.user.dto.response.AuthorResponse;
import ams.asmtravel.api.services.user.user.dto.request.UserRequest;
import ams.asmtravel.api.services.user.user.dto.response.UserResponse;
import org.springframework.security.core.Authentication;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * All users, customers and staff, and the signed-in person.
 * A row is made from a person's first token (see {@link #requireUserId}).
 */
public interface UserService {

    /** Creates a customer (name and email required), with a Keycloak sign-in when available. */
    UserResponse createNew(UserRequest request);

    List<UserResponse> getAll();

    UserResponse findById(UUID id);

    /** Edits a profile and syncs the customer sign-in; null fields stay unchanged. */
    UserResponse updateById(UUID id, UserRequest request);

    /**
     * Deletes a person, their data in other services (see {@link ams.asmtravel.api.services.user.user.domain.UserDeletingEvent})
     * and their sign-in. 409 while they have publish requests.
     */
    void deleteById(UUID id);

    /** Their own profile; refuses an inactive one. */
    UserResponse findMine(UUID userId);

    /** Updates their own name (required) and picture; an empty avatar key removes the picture. */
    UserResponse updateMine(UUID userId, UserRequest request);

    /**
     * The signed-in person's id: by realm and subject, else by email, else a new row. Call it from a controller.
     *
     * @throws org.springframework.web.server.ResponseStatusException 401 when nobody is signed in, the token has no email,
     *         or the person is inactive
     */
    UUID requireUserId(Authentication authentication);

    /** The same, or empty for an anonymous caller — for endpoints anyone may call. */
    Optional<UUID> findUserId(Authentication authentication);

    /** The id {@link #requireUserId} found in this request, without a DB call; for flush-time code like the audit log. */
    Optional<UUID> resolvedUserId();

    /**
     * The row, for another service's relation to it.
     *
     * @throws org.springframework.web.server.ResponseStatusException 404 when there is none
     */
    User getEntity(UUID id);

    boolean exists(UUID id);

    Optional<UserSummary> findSummary(UUID id);

    /** Those of these people who exist, by id. */
    Map<UUID, UserSummary> findSummaries(Collection<UUID> ids);

    /** Who holds any of these roles, as of their last sign-in. */
    Set<UUID> findUserIdsHoldingAny(Set<RoleCode> roles);

    /** Everyone who writes catalogue rows — the back-office accounts — as a name beside an id. */
    List<AuthorResponse> findAuthors();

    /** Makes the mirrored roles of the person behind a staff account match Keycloak's, if they have signed in. */
    void mirrorBackofficeRoles(String keycloakId, Set<RoleCode> roles);

    /** How many there are: a tile on the admin overview. */
    long count();
}
