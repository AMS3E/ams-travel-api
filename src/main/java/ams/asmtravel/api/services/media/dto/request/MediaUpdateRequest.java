package ams.asmtravel.api.services.media.dto.request;

import ams.asmtravel.api.services.media.domain.MediaStatus;
import jakarta.validation.constraints.Size;

/**
 * Changes to a gallery item; a null field keeps its value.
 *
 * @param caption blank clears it
 * @param isCover true makes it the owner's only cover
 */
public record MediaUpdateRequest(
        @Size(max = 255) String caption,
        Integer sortOrder,
        Boolean isCover,
        MediaStatus status
) {
}
