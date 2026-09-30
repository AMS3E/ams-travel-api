package ams.asmtravel.api.services.history.regioncoverage.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * @param historicalPeriodName beside its id, so the era reads without a second lookup
 * @param destinationSlug null when there is no place to open
 */
public record RegionCoverageResponse(
        UUID id,
        UUID regionId,
        Integer sortOrder,
        String title,
        String titleKm,
        String dateRange,
        UUID historicalPeriodId,
        String historicalPeriodName,
        String description,
        UUID destinationId,
        String destinationSlug,
        String destinationName,
        /** When it was made: what "Newest first" sorts on. */
        Instant createdAt
) {
}
