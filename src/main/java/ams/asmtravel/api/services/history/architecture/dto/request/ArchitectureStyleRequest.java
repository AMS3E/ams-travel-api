package ams.asmtravel.api.services.history.architecture.dto.request;

import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** @param periodId the historical period it belongs to; null for none */
public record ArchitectureStyleRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        UUID periodId,
        @Size(max = 512)
        @MediaKey(allowEmpty = true, message = "Upload the image first, then send its key")
        String imageKey
) {
}
