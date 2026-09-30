package ams.asmtravel.api.services.user.user.dto.request;

import ams.asmtravel.api.services.media.dto.request.MediaKey;
import ams.asmtravel.api.services.user.user.domain.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Builder;


@Builder
public record UserRequest(
        @Size(max = 255) String fullName,
        @Email @Size(max = 255) String email,
        @Size(max = 50) String phone,
        @MediaKey(allowEmpty = true, message = "Upload the picture first, then send its key")
        String avatarKey,
        @Size(min = 8, max = 128, message = "Use at least eight characters") String password,
        UserStatus status,
        Boolean temporaryPassword
) {
}
