package ams.asmtravel.api.services.user.user.dto.request;

import java.util.Set;

import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record StaffRequest(
        @Size(min = 3, max = 64)
        @Pattern(regexp = "^[a-z0-9._@-]+$", message = "Use lowercase letters, digits, and . _ - @ only")
        String username,

        @Email String email,
        @Size(max = 64) String firstName,
        @Size(max = 64) String lastName,

        @Size(min = 8, max = 128, message = "Use at least eight characters")
        String password,

        Boolean temporaryPassword,
        Set<RoleCode> roles
) {
}
