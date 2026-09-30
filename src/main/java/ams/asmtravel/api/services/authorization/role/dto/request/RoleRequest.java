package ams.asmtravel.api.services.authorization.role.dto.request;

/**
 * Which roles to list, as query parameters.
 *
 * @param code one role, such as {@code EDITOR}; every role when null
 */
public record RoleRequest(String code) {
}
