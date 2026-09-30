package ams.asmtravel.api.services.season.dto.response;

import ams.asmtravel.api.services.season.domain.SeasonStatus;
import ams.asmtravel.api.services.season.domain.SeasonType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SeasonResponse(
        UUID id,
        String name,
        SeasonType type,
        LocalDate startsOn,
        LocalDate endsOn,
        Boolean isActive,
        SeasonStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
