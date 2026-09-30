package ams.asmtravel.api.services.history.period.dto.request;

import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param startYear and {@code endYear} are CE years; null for an open end */
public record HistoricalPeriodRequest(
        @NotBlank @Size(max = 150) String name,
        Integer startYear,
        Integer endYear,
        String description,
        @Size(max = 512)
        @MediaKey(allowEmpty = true, message = "Upload the cover first, then send its key")
        String coverKey
) {
}
