package ams.asmtravel.api.services.user.user.domain;

import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import java.util.Set;
import java.util.UUID;

/** A person as other services show them (e.g. a review's author), without the entity. */
public record UserSummary(
        UUID id,
        String fullName,
        String email,
        String avatarKey,
        Set<RoleCode> roles
) {
    public UserSummary {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
