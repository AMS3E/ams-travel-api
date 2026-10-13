package ams.asmtravel.api.services.taxonomy.amenity.dto.request;

import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.taxonomy.amenity.domain.AmenityLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * An amenity, for create and edit.
 *
 * @param slug      null or empty: made from the name (new) or kept (edit)
 * @param groupName the heading it is picked under
 * @param iconKey   an icon name; null draws a plain tick
 * @param level     PROPERTY, ROOM or BOTH
 * @param sortOrder its place within its group, 0 first; null puts a new one last and keeps an edited one in place
 * @param isActive  null keeps it (new: active)
 */
public record AmenityRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) @Pattern(regexp = "^$|" + Slugs.PATTERN, message = "Use lower-case words joined by hyphens")
        String slug,
        @NotBlank @Size(max = 100) String groupName,
        @Size(max = 60) String iconKey,
        @NotNull AmenityLevel level,
        @PositiveOrZero Integer sortOrder,
        Boolean isActive
) {
}
