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
        /** "What you expect" on its page: short labels typed by the editor, in the editor's order. */
        List<String> expectations,
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
        Integer sortOrder,
        /** The whole route, Google-encoded, when it has been calculated: one line on the country map. */
        String encodedPolyline,
        /** Its stops in travel order: the names under the card, and what the province and region filters read. */
        List<String> stopNames,
        List<UUID> stopDestinationIds,
        List<UUID> stopProvinceIds,
        /** The zones of its stops' provinces and the tourism regions its stops are shelved in. */
        List<UUID> stopRegionIds,
        /** Khmer twins of the name and descriptions; null until written. */
        String nameKm,
        String shortDescriptionKm,
        String descriptionKm
) {
}
