package ams.asmtravel.api.services.destination.destination.dto.response;

import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param regionId         the zone, through the province
 * @param tourismRegionIds the tourism regions chosen for it
 */
public record DestinationResponse(
        UUID id,
        String name,
        String slug,
        UUID provinceId,
        UUID regionId,
        String shortDescription,
        String description,
        String slogan,
        String sloganBy,
        BigDecimal latitude,
        BigDecimal longitude,
        String address,
        String coverKey,
        String coverUrl,
        ListingStatus status,
        Long viewCount,
        UUID createdBy,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        List<UUID> categoryIds,
        List<UUID> tagIds,
        List<UUID> interestIds,
        List<UUID> tourismRegionIds,
        String theme,
        List<String> specialties
) {
}
