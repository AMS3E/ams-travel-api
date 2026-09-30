package ams.asmtravel.api.services.media.dto.request;

import ams.asmtravel.api.services.media.domain.MediaType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * New gallery photos: each file sent is stored and becomes one photo;
 * without files, {@code mediaKey} names an earlier upload.
 *
 * @param ownerType DESTINATION, STAY or FOOD
 * @param sortOrder the first photo's place; null puts them after the last one
 * @param isCover   true makes the first photo the owner's only cover
 */
public record MediaUploadRequest(
        @NotNull MediaType ownerType,
        @NotNull UUID ownerId,
        @Size(max = 512) String mediaKey,
        @Size(max = 255) String caption,
        Integer sortOrder,
        Boolean isCover
) {
    @JsonIgnore
    @AssertTrue(message = "Upload the picture first, then send its key")
    public boolean isKeyUploaded() {
        return mediaKey == null || mediaKey.isBlank() || MediaKey.Validator.isImageKey(mediaKey.trim());
    }
}
