package ams.asmtravel.api.services.destination.destination.dto.response;

import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param regionId         the zone, through the province
 * @param tourismRegionIds the tourism regions chosen for it
 * @param missingKhmer     Khmer fields still empty while their English is filled; publishing waits on them
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
        List<UUID> architectureStyleIds,
        List<UUID> religionIds,
        String theme,
        List<String> specialties,
        List<UUID> historicalPeriodIds,
        List<Integer> centuries,
        String nameKm,
        String descriptionKm,
        String sloganKm,
        String sloganByKm,
        String addressKm,
        List<String> missingKhmer
) {
}
