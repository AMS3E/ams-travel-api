package ams.asmtravel.api.services.review.review.dto.response;

import ams.asmtravel.api.services.review.review.domain.ReviewStatus;
import ams.asmtravel.api.services.review.review.domain.ReviewTargetType;
import ams.asmtravel.api.services.media.dto.response.MediaResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A review with its author's name and picture as their profile has them now, and its photos in order. */
public record ReviewResponse(
        UUID id,
        UUID userId,
        String authorName,
        String authorAvatarUrl,
        ReviewTargetType targetType,
        UUID targetId,
        Integer rating,
        String title,
        String content,
        ReviewStatus status,
        String moderationNote,
        Instant moderatedAt,
        List<MediaResponse> photos,
        Instant createdAt,
        Instant updatedAt
) {
}
