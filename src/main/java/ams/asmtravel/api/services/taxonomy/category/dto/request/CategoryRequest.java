package ams.asmtravel.api.services.taxonomy.category.dto.request;

import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param slug     null or empty: made from the name
 * @param coverKey the uploaded icon picture's key (folder {@code category}); null keeps the current one, empty removes it
 */
public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) @Pattern(regexp = "^$|" + Slugs.PATTERN, message = "Use lower-case words joined by hyphens")
        String slug,
        String description,
        @Size(max = 512) String iconKey,
        @MediaKey(allowEmpty = true, message = "Upload the icon first, then send its key") String coverKey,
        Integer sortOrder,
        Boolean isActive,
        /** Khmer twins: null keeps what is stored, blank clears it. */
        @Size(max = 100) String nameKm
) {
}
