package ams.asmtravel.api.services.history.history.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** @param historicalPeriodName beside its id, so a timeline reads without a second lookup */
public record DestinationHistoryResponse(
        UUID id,
        UUID destinationId,
        UUID historicalPeriodId,
        String historicalPeriodName,
        String title,
        String description,
        Integer eventYear,
        LocalDate startDate,
        LocalDate endDate,
        String sourceReference,
        Instant createdAt,
        Instant updatedAt
) {
}
