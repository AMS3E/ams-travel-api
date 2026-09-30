package ams.asmtravel.api.services.authorization.role.dto.request;

import java.util.UUID;

/**
 * Which held roles to list, as query parameters.
 *
 * @param userId one person's roles; every person's when null
 */
public record UserRoleRequest(UUID userId) {
}
