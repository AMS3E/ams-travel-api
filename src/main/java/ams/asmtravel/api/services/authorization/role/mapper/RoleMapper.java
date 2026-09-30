package ams.asmtravel.api.services.authorization.role.mapper;

import ams.asmtravel.api.services.authorization.role.domain.Role;
import ams.asmtravel.api.services.authorization.role.dto.response.RoleResponse;
import ams.asmtravel.api.services.authorization.permission.domain.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    @Mapping(target = "permissions", expression = "java(permissionCodes(role))")
    RoleResponse toResponse(Role role);

    default List<String> permissionCodes(Role role) {
        return role.getPermissions().stream().map(Permission::getCode).sorted().toList();
    }
}
