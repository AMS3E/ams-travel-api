package ams.asmtravel.api.services.history.regioncoverage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * One entry of a tourism region's historical coverage (a royal capital).
 *
 * @param sortOrder     position in the list, 0 first; null puts a new entry last and keeps an edited one in place
 * @param dateRange     free text, e.g. "c. 600 – 700 CE"
 * @param destinationId the place to open, or null
 */
public record RegionCoverageRequest(
        @NotNull UUID regionId,
        Integer sortOrder,
        @NotBlank @Size(max = 150) String title,
        @Size(max = 150) String titleKm,
        @Size(max = 100) String dateRange,
        UUID historicalPeriodId,
        String description,
        UUID destinationId
) {
}
