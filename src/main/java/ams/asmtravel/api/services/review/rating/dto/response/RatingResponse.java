package ams.asmtravel.api.services.review.rating.dto.response;

import ams.asmtravel.api.services.review.rating.domain.RatingStatus;
import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;

import java.time.Instant;
import java.util.UUID;

public record RatingResponse(
        UUID id,
        UUID userId,
        RatingTargetType targetType,
        UUID targetId,
        Integer stars,
        RatingStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
