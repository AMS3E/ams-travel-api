package ams.asmtravel.api.services.badge.dto.response;

import ams.asmtravel.api.services.badge.domain.BadgeLevel;
import ams.asmtravel.api.services.badge.domain.BadgeType;

import java.time.Instant;
import java.util.UUID;

/** A person's badge in one season; the id doubles as its share link. */
public record UserBadgeResponse(
        UUID id,
        UUID badgeId,
        String code,
        String name,
        String iconKey,
        BadgeType type,
        BadgeLevel level,
        Integer progress,
        Integer threshold,
        UUID seasonId,
        String seasonName,
        String holderName,
        Instant earnedAt
) {
}
