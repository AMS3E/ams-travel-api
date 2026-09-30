package ams.asmtravel.api.services.point.dto.response;

import ams.asmtravel.api.services.point.domain.PointSourceType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PointTransactionResponse(
        UUID id,
        String targetType,
        UUID targetId,
        PointSourceType sourceType,
        UUID sourceId,
        UUID seasonId,
        int points,
        LocalDate rewardDate,
        Instant createdAt
) {
}
