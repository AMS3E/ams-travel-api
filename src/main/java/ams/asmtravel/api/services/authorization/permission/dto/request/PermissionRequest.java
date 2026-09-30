package ams.asmtravel.api.services.authorization.permission.dto.request;

/**
 * Which permissions to list, as query parameters.
 *
 * @param code one permission, such as {@code PERM_DESTINATION_VERIFY}; every permission when null
 */
public record PermissionRequest(String code) {
}
