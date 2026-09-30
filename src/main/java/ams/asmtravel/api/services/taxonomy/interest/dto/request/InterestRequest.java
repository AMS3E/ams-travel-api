package ams.asmtravel.api.services.taxonomy.interest.dto.request;

import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * @param slug      null or empty: made from the name
 * @param groupName the Discover group; null leaves it off those lists
 * @param sortOrder place in its group, 0 first; null puts a new one last and keeps an edited one in place
 */
public record InterestRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) @Pattern(regexp = "^$|" + Slugs.PATTERN, message = "Use lower-case words joined by hyphens")
        String slug,
        String description,
        @Size(max = 60) String groupName,
        @Size(max = 512) String iconKey,
        Boolean isActive,
        @PositiveOrZero Integer sortOrder
) {
}
