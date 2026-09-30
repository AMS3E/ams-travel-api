package ams.asmtravel.api.services.user.user.service;

import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.user.user.dto.request.StaffRequest;
import ams.asmtravel.api.services.user.user.dto.response.StaffResponse;

import java.util.List;
import java.util.Set;

/** Staff accounts, which live in Keycloak's back-office realm. */
public interface StaffService {

    /** Everyone holding at least one back-office role. */
    List<StaffResponse> findBackofficeUsers();

    /** Anyone in the realm matching the query, back-office or not. */
    List<StaffResponse> search(String query);

    StaffResponse findById(String id);

    StaffResponse create(StaffRequest request);

    /** Sets exactly these roles, adding and revoking to match, in Keycloak and the mirror. */
    StaffResponse setRoles(String id, Set<RoleCode> roles);
}
