package ams.asmtravel.api.services.user.user.domain;


import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** A staff account in Keycloak; {@code id} is the Keycloak id, not an {@code auth.users} id. */
public record BackofficeUser(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        Set<RoleCode> roles,
        Instant createdAt
) {
    public BackofficeUser {
        roles = roles == null || roles.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(roles));
    }
}
