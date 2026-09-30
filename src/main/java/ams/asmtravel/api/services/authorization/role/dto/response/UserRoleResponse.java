package ams.asmtravel.api.services.authorization.role.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * A role a person holds, as of their last sign-in.
 *
 * @param assignedAt when the database first saw the role
 */
public record UserRoleResponse(
        UUID userId,
        UUID roleId,
        String roleCode,
        String roleName,
        Instant assignedAt
) {
}
