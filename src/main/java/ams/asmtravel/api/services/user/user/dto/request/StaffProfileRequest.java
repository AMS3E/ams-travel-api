package ams.asmtravel.api.services.user.user.dto.request;

import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * An edited staff account, sent whole: the name, e-mail and whether it may sign in go to Keycloak, the phone and
 * photo to the person's profile here. An empty phone or photo removes it. Roles and the password are not here.
 *
 * @param avatarKey the uploaded photo's key; empty removes the photo
 */
public record StaffProfileRequest(
        @NotBlank @Size(max = 64) String firstName,
        @Size(max = 64) String lastName,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 50) String phone,
        @MediaKey(allowEmpty = true, message = "Upload the photo first, then send its key") String avatarKey,
        @NotNull Boolean enabled
) {
}
