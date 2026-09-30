package ams.asmtravel.api.services.badge.dto.request;

import ams.asmtravel.api.services.badge.domain.BadgeLevel;
import ams.asmtravel.api.services.badge.domain.BadgeStatus;
import ams.asmtravel.api.services.badge.domain.BadgeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/** The badge's new values; a null status keeps it. */
public record UpdateBadgeRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[A-Za-z0-9]+(?:_[A-Za-z0-9]+)*$",
                message = "Use letters and digits, words joined by underscores") String code,
        @NotBlank @Size(max = 150) String name,
        String description,
        @Size(max = 512) String iconKey,
        @NotNull BadgeType type,
        BadgeLevel level,
        @NotNull @Min(1) Integer threshold,
        BadgeStatus status,
        Set<UUID> destinationIds
) {
}
