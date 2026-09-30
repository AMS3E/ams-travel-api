package ams.asmtravel.api.services.review.rating.dto.response;

import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** A place's average (null when unrated) and counts per star. */
public record RatingSummaryResponse(
        RatingTargetType targetType,
        UUID targetId,
        BigDecimal average,
        long count,
        Map<String, Long> stars
) {
}
