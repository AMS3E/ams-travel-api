package ams.asmtravel.api.services.authorization.role.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.authorization.role.dto.request.RoleRequest;
import ams.asmtravel.api.services.authorization.role.dto.response.RoleResponse;
import ams.asmtravel.api.services.authorization.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The realm's roles and what each carries. Read-only: roles are changed in Keycloak. */
@Tag(name = "People · Roles", description = "The roles the system knows.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public List<RoleResponse> getAll(RoleRequest request) {
        return roleService.getAll(request);
    }
}
