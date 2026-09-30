package ams.asmtravel.api.services.authorization.permission.service;

import ams.asmtravel.api.services.authorization.permission.dto.request.PermissionRequest;
import ams.asmtravel.api.services.authorization.permission.dto.response.PermissionResponse;

import java.util.List;

/** The realm's {@code PERM_} roles, as mirrored in the database. Read-only. */
public interface PermissionService {

    /** By code; one permission when the request names it. */
    List<PermissionResponse> getAll(PermissionRequest request);
}
