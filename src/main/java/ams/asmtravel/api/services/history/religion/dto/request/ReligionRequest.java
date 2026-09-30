package ams.asmtravel.api.services.history.religion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param iconKey an icon's name ({@code lotus}), not an upload */
public record ReligionRequest(
        @NotBlank @Size(max = 150) String name,
        String description,
        @Size(max = 512) String iconKey
) {
}
