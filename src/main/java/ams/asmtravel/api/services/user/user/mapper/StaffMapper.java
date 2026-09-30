package ams.asmtravel.api.services.user.user.mapper;

import ams.asmtravel.api.services.user.user.domain.BackofficeUser;
import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.user.user.dto.request.StaffRequest;
import ams.asmtravel.api.services.user.user.dto.response.StaffResponse;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.Mapper;

import java.time.Instant;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface StaffMapper {

    StaffResponse toBackofficeUserResponse(BackofficeUser user);

    default BackofficeUser toDomain(StaffRequest request) {
        return new BackofficeUser(null, request.username(), request.email(), request.firstName(),
                request.lastName(), true, request.roles(), null);
    }

    default BackofficeUser toDomain(UserRepresentation user, Set<RoleCode> roles) {
        return new BackofficeUser(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                Boolean.TRUE.equals(user.isEnabled()),
                roles,
                user.getCreatedTimestamp() == null ? null : Instant.ofEpochMilli(user.getCreatedTimestamp()));
    }

    /** New staff accounts start verified: an administrator typed the address. */
    default UserRepresentation toRepresentation(BackofficeUser account) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(account.username());
        user.setEmail(account.email());
        user.setFirstName(account.firstName());
        user.setLastName(account.lastName());
        user.setEnabled(account.enabled());
        user.setEmailVerified(true);
        return user;
    }
}
