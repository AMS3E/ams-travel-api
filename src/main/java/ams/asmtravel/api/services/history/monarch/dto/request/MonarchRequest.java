package ams.asmtravel.api.services.history.monarch.dto.request;

import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param reignStartYear and {@code reignEndYear} are CE years; null when not known */
public record MonarchRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 150) String title,
        Integer reignStartYear,
        Integer reignEndYear,
        String description,
        @Size(max = 512)
        @MediaKey(allowEmpty = true, message = "Upload the image first, then send its key")
        String imageKey
) {
}
