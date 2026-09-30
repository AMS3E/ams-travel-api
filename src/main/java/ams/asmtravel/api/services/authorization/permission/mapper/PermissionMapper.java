package ams.asmtravel.api.services.authorization.permission.mapper;

import ams.asmtravel.api.services.authorization.permission.domain.Permission;
import ams.asmtravel.api.services.authorization.permission.dto.response.PermissionResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PermissionMapper {

    PermissionResponse toResponse(Permission permission);
}
