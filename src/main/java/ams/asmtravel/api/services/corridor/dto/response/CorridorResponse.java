package ams.asmtravel.api.services.corridor.dto.response;

import ams.asmtravel.api.services.corridor.domain.CorridorRouteStatus;
import ams.asmtravel.api.services.corridor.domain.CorridorStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Corridor overview; days, stops and the road line are at {@code GET /corridors/{id}/itinerary}.
 *
 * @param isActive        true when {@code status} is PUBLISHED
 * @param totalDistanceKm null until a route is calculated
 * @param routeError      last route failure, for the CMS
 * @param sortOrder       list position, 0 first
 */
public record CorridorResponse(
        UUID id,
        String name,
        String slug,
        String shortDescription,
        String description,
        String coverKey,
        String coverUrl,
        CorridorStatus status,
        Boolean isActive,
        Integer durationDays,
        Integer durationNights,
        int stopCount,
        BigDecimal totalDistanceKm,
        Integer totalDurationMinutes,
        BigDecimal estimatedBudgetMin,
        BigDecimal estimatedBudgetMax,
        UUID startDestinationId,
        UUID endDestinationId,
        CorridorRouteStatus routeStatus,
        Instant routeCalculatedAt,
        String routeError,
        Instant publishedAt,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt,
        List<UUID> interestIds,
        Integer sortOrder
) {
}
