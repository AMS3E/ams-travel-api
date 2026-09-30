package ams.asmtravel.api.services.corridor.dto.request;

import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * Corridor overview for create and edit. Days and stops are set via {@code PUT /corridors/{id}/itinerary}.
 *
 * @param slug               null or empty: made from the name
 * @param estimatedBudgetMin per person, in US dollars
 * @param interestIds        Tourism Corridors interests; null keeps them on edit
 * @param sortOrder          0 is first; null puts a new one last and keeps an edited one in place
 */
public record CorridorRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 120) @Pattern(regexp = "^$|" + Slugs.PATTERN, message = "Use lower-case words joined by hyphens")
        String slug,
        @Size(max = 500) String shortDescription,
        String description,
        @MediaKey(allowEmpty = true, message = "Upload the cover first, then send its key")
        String coverKey,
        @DecimalMin("0") @DecimalMax("99999999") BigDecimal estimatedBudgetMin,
        @DecimalMin("0") @DecimalMax("99999999") BigDecimal estimatedBudgetMax,
        Boolean isFeatured,
        Set<UUID> interestIds,
        @PositiveOrZero Integer sortOrder
) {
}
