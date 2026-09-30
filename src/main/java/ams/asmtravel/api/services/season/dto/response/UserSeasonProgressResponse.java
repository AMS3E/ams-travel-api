package ams.asmtravel.api.services.season.dto.response;

import ams.asmtravel.api.services.season.domain.SeasonType;
import ams.asmtravel.api.services.season.domain.SeasonStatus;

import java.time.Instant;
import java.util.UUID;

/** A person's totals in one season; zeros until their first activity in it. */
public record UserSeasonProgressResponse(
        UUID seasonId,
        String seasonName,
        SeasonType seasonType,
        SeasonStatus seasonStatus,
        int points,
        int ratingCount,
        int reviewCount,
        int uniqueRatedTargetCount,
        int uniqueReviewedTargetCount,
        Instant startedAt,
        Instant updatedAt
) {
}
