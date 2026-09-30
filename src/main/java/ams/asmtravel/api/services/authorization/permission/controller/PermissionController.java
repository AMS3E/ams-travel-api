package ams.asmtravel.api.services.authorization.permission.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.authorization.permission.dto.request.PermissionRequest;
import ams.asmtravel.api.services.authorization.permission.dto.response.PermissionResponse;
import ams.asmtravel.api.services.authorization.permission.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** The realm's {@code PERM_} roles. Read-only. */
@Tag(name = "People · Permissions", description = "The permissions roles are built from.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public List<PermissionResponse> getAll(PermissionRequest request) {
        return permissionService.getAll(request);
    }
}
