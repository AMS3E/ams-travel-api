package ams.asmtravel.api.services.badge.dto.response;

import ams.asmtravel.api.services.badge.domain.BadgeLevel;
import ams.asmtravel.api.services.badge.domain.BadgeStatus;
import ams.asmtravel.api.services.badge.domain.BadgeType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** @param level null for a collection badge, which is on no ladder */
public record BadgeResponse(
        UUID id,
        String code,
        String name,
        String description,
        String iconKey,
        BadgeType type,
        BadgeLevel level,
        Integer threshold,
        BadgeStatus status,
        Set<UUID> destinationIds,
        Instant createdAt,
        Instant updatedAt
) {
}
