package ams.asmtravel.api.services.authorization.permission.dto.response;

import java.util.UUID;

public record PermissionResponse(
        UUID id,
        String code,
        String name,
        String description
) {
}
