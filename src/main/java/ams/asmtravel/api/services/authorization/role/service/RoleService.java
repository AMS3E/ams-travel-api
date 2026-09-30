package ams.asmtravel.api.services.authorization.role.service;

import ams.asmtravel.api.services.authorization.role.domain.Role;
import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.authorization.role.dto.request.RoleRequest;
import ams.asmtravel.api.services.authorization.role.dto.response.RoleResponse;

import java.util.Collection;
import java.util.List;

/** The roles the realm defines and the permissions each carries, as mirrored in the database. Read-only. */
public interface RoleService {

    /** Lowest first ({@code USER} … {@code ADMIN}); one role when the request names it. */
    List<RoleResponse> getAll(RoleRequest request);

    /* For other services. */

    /** Rows for these codes; missing ones are skipped. */
    List<Role> findByCodes(Collection<RoleCode> codes);
}
