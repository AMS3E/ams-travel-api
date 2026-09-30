package ams.asmtravel.api.services.history.period.dto.response;

import java.time.Instant;
import java.util.UUID;

public record HistoricalPeriodResponse(
        UUID id,
        String name,
        Integer startYear,
        Integer endYear,
        String description,
        String coverKey,
        String coverUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
