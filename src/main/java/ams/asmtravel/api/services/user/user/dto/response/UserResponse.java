package ams.asmtravel.api.services.user.user.dto.response;

import ams.asmtravel.api.services.user.user.domain.UserStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param avatarKey what a form sends back to keep the picture
 * @param avatarUrl where to fetch it; null without one
 * @param roles     role codes, lowest first ({@code USER} … {@code ADMIN})
 */
public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String avatarKey,
        String avatarUrl,
        String phone,
        UserStatus status,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt,
        List<String> roles
) {
}
