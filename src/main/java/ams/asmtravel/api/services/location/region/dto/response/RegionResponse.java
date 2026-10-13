package ams.asmtravel.api.services.location.region.dto.response;

import ams.asmtravel.api.services.location.region.domain.RegionKind;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param kind ZONE (geography the provinces sit in) or TOURISM (a theme)
 * @param categoryIds the categories a tourism region's shelves are built from, in its order
 * @param tagIds the filter chips it offers, in its order
 * @param sortOrder its place among the regions of its kind, 0 for the first
 * @param interestIds the interests an attraction of its theme is offered
 */
public record RegionResponse(
        UUID id,
        String name,
        String code,
        RegionKind kind,
        UUID parentId,
        String description,
        String historyIntro,
        String coverKey,
        String coverUrl,
        List<UUID> categoryIds,
        List<UUID> tagIds,
        List<UUID> interestIds,
        Boolean isActive,
        Integer sortOrder,
        Instant createdAt
) {
}
