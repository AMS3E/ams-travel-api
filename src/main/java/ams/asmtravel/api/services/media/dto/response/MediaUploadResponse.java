package ams.asmtravel.api.services.media.dto.response;

import lombok.Builder;

/**
 * One uploaded file.
 *
 * @param key          what a row stores ({@code coverKey}, {@code avatarKey}…)
 * @param uri          where to fetch it
 * @param mimeTypeFile the type read from the file's bytes
 */
@Builder
public record MediaUploadResponse(
        String key,
        String uri,
        String name,
        String extension,
        String mimeTypeFile,
        Long size,
        String category
) {
}
