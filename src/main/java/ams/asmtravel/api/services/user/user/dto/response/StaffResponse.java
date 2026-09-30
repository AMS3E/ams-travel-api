package ams.asmtravel.api.services.user.user.dto.response;

import ams.asmtravel.api.services.authorization.role.domain.RoleCode;

import java.time.Instant;
import java.util.Set;

/** A staff account; {@code id} is the Keycloak id. Only back-office roles are listed, not {@code PERM_} ones. */
public record StaffResponse(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        Set<RoleCode> roles,
        Instant createdAt
) {
}
