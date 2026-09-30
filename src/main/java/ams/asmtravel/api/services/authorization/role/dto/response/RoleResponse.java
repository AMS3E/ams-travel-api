package ams.asmtravel.api.services.authorization.role.dto.response;

import java.util.List;
import java.util.UUID;

/** @param permissions the codes of the permissions the role carries, sorted */
public record RoleResponse(
        UUID id,
        String code,
        String name,
        String description,
        List<String> permissions
) {
}
