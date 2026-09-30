package ams.asmtravel.api.services.taxonomy.tag.dto.request;

import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** @param slug null or empty: made from the name */
public record TagRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) @Pattern(regexp = "^$|" + Slugs.PATTERN, message = "Use lower-case words joined by hyphens")
        String slug,
        String description,
        Boolean isActive
) {
}
