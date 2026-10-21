package ams.asmtravel.api.services.location.region.dto.response;

import ams.asmtravel.api.services.location.region.domain.RegionKind;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param kind ZONE (geography the provinces sit in) or TOURISM (a theme)
 * @param categoryIds the categories a tourism region's shelves are built from, in its order
 * @param tagIds the filter chips it offers, in its order
 * @param sortOrder its place among the regions of its kind, 0 for the first
 * @param interestIds the interests an attraction of its theme is offered
 * @param latitude    its centre, the marker on the Map screen: the one an editor set, else the middle of its places
 * @param centreSet   true when an editor set the centre; false when it is the middle of the places and follows them
 * @param bounds      the box around its published places, for a map to frame; null with none
 */
public record RegionResponse(
        UUID id,
        String name,
        String code,
        RegionKind kind,
        UUID parentId,
        String description,
        String coverKey,
        String coverUrl,
        /** The Map panel picture on its page in the app, in place of the interactive map; null until one is uploaded. */
        String mapKey,
        String mapUrl,
        List<UUID> categoryIds,
        List<UUID> tagIds,
        List<UUID> interestIds,
        Boolean isActive,
        /** Recommended by AMS Travel: staff chose it, and the app shows the badge on its card. */
        boolean amsRecommended,
        Integer sortOrder,
        BigDecimal latitude,
        BigDecimal longitude,
        boolean centreSet,
        GeoBounds bounds,
        Instant createdAt,
        /** Khmer name and description; null until an editor writes them. */
        String nameKm,
        String descriptionKm
) {
}
