package ams.asmtravel.api.services.media.dto.response;

import ams.asmtravel.api.services.media.domain.MediaStatus;
import ams.asmtravel.api.services.media.domain.MediaType;

import java.time.Instant;
import java.util.UUID;

/** One gallery item, with the link to its file. */
public record MediaResponse(
        UUID id,
        MediaType ownerType,
        UUID ownerId,
        String mediaKey,
        String mediaUrl,
        MediaStatus status,
        String caption,
        Integer sortOrder,
        Boolean isCover,
        Instant createdAt
) {
}
