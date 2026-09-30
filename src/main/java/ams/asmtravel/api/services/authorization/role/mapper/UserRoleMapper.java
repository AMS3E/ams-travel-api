package ams.asmtravel.api.services.authorization.role.mapper;

import ams.asmtravel.api.services.authorization.role.domain.UserRole;
import ams.asmtravel.api.services.authorization.role.dto.response.UserRoleResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserRoleMapper {

    @Mapping(target = "userId", source = "id.userId")
    @Mapping(target = "roleId", source = "id.roleId")
    @Mapping(target = "roleCode", source = "role.code")
    @Mapping(target = "roleName", source = "role.name")
    UserRoleResponse toResponse(UserRole userRole);
}
