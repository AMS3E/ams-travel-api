package ams.asmtravel.api.services.authorization.role.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.authorization.role.dto.request.UserRoleRequest;
import ams.asmtravel.api.services.authorization.role.dto.response.UserRoleResponse;
import ams.asmtravel.api.services.authorization.role.service.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Who holds which role, as of their last sign-in. Read-only: roles are changed in Keycloak. */
@Tag(name = "People · User roles", description = "Who holds which role.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user-roles")
public class UserRoleController {

    private final UserRoleService userRoleService;

    @GetMapping
    public List<UserRoleResponse> getAll(UserRoleRequest request) {
        return userRoleService.getAll(request);
    }
}
