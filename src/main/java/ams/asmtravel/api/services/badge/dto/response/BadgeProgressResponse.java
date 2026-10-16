package ams.asmtravel.api.services.badge.dto.response;

import ams.asmtravel.api.services.badge.domain.BadgeLevel;
import ams.asmtravel.api.services.badge.domain.BadgeType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A person's badges in one season; earned holds what this call unlocked. */
public record BadgeProgressResponse(
        UUID seasonId,
        String seasonName,
        List<Track> tracks,
        List<UserBadgeResponse> badges,
        List<UserBadgeResponse> earned,
        Boolean browseCounted,
        /** Where the traveller stands among the season's active members: 5 means the top 5%. Null with no activity yet. */
        Integer percentile
) {

    /** One ladder this season; nextLevel and remaining are null at the top. */
    public record Track(
            BadgeType type,
            long count,
            BadgeLevel level,
            BadgeLevel nextLevel,
            Long remaining,
            Map<BadgeLevel, Integer> thresholds
    ) {
    }
}
