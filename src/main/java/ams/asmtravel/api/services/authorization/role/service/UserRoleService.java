package ams.asmtravel.api.services.authorization.role.service;

import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.authorization.role.dto.request.UserRoleRequest;
import ams.asmtravel.api.services.authorization.role.dto.response.UserRoleResponse;
import ams.asmtravel.api.services.user.user.domain.User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Roles people hold, copied from Keycloak at each sign-in. Roles change only in Keycloak. */
public interface UserRoleService {

    /** The held roles, newest first; one person's when the request names them. */
    List<UserRoleResponse> getAll(UserRoleRequest request);

    /* For other services. */

    /** Held roles in {@link RoleCode} order; unknown roles are skipped. */
    Set<RoleCode> rolesOf(UUID userId);

    /** Roles per person; people with none are left out. */
    Map<UUID, Set<RoleCode>> rolesOf(Collection<UUID> userIds);

    /** Sets the person's roles to {@code wanted}; returns true if anything changed. */
    boolean replace(User user, Set<RoleCode> wanted);

    /** Who holds any of these roles, as of their last sign-in. */
    Set<UUID> findUserIdsHoldingAny(Set<RoleCode> roles);
}
