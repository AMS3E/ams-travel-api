package ams.asmtravel.api.services.taxonomy.interest.dto.request;

import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * @param slug      null or empty: made from the name
 * @param groupName the Discover group; null leaves it off those lists
 * @param iconKey   an icon's name; null keeps the current one, empty removes it
 * @param coverKey  the uploaded picture's key (folder {@code interest}); null keeps the current one, empty removes it
 * @param sortOrder place in its group, 0 first; null puts a new one last and keeps an edited one in place
 */
public record InterestRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) @Pattern(regexp = "^$|" + Slugs.PATTERN, message = "Use lower-case words joined by hyphens")
        String slug,
        String description,
        @Size(max = 60) String groupName,
        @Size(max = 512) String iconKey,
        @MediaKey(allowEmpty = true, message = "Upload the picture first, then send its key") String coverKey,
        Boolean isActive,
        @PositiveOrZero Integer sortOrder,
        /** Khmer twins: null keeps what is stored, blank clears it. */
        @Size(max = 100) String nameKm,
        String descriptionKm
) {
}
