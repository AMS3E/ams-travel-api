package ams.asmtravel.api.services.review.review.dto.response;

import ams.asmtravel.api.services.review.review.domain.ReviewTargetType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A place's published review count and average stars. */
public record ReviewSummaryResponse(
        ReviewTargetType targetType,
        UUID targetId,
        long reviewCount,
        Instant latestReviewAt,
        BigDecimal averageRating,
        long ratingCount
) {
}
