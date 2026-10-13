package ams.asmtravel.api.services.destination.destination.dto.request;

import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A destination, for create and edit. Status is not here: it changes by publish/unpublish.
 * On an edit, a null list keeps the links and an empty one clears them.
 *
 * @param slug             blank: made from the name on create, kept on edit
 * @param slogan           one line someone said about it, quoted on its page; sloganBy says who
 * @param tourismRegionIds zones are refused
 * @param theme            main tourism region code, see {@link ams.asmtravel.api.services.destination.destination.domain.DestinationThemes}
 * @param centuries        1 to 21
 */
public record DestinationRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 150) @Pattern(regexp = "^$|" + Slugs.PATTERN, message = "Use lower-case words joined by hyphens")
        String slug,
        @NotNull UUID provinceId,
        @Size(max = 500) String shortDescription,
        String description,
        @Size(max = 300) String slogan,
        @Size(max = 120) String sloganBy,
        @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
        @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
        String address,
        @MediaKey(allowEmpty = true, message = "Upload the cover first, then send its key")
        String coverKey,
        Set<UUID> categoryIds,
        Set<UUID> tagIds,
        Set<UUID> interestIds,
        Set<UUID> tourismRegionIds,
        Set<UUID> architectureStyleIds,
        Set<UUID> religionIds,
        @Size(max = 40) String theme,
        List<@NotBlank @Size(max = 100) String> specialties,
        Set<UUID> historicalPeriodIds,
        List<@NotNull @Min(1) @Max(21) Integer> centuries
) {
}
