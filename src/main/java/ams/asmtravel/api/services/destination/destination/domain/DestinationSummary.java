package ams.asmtravel.api.services.destination.destination.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** A light copy of a destination for other services, without the entity's lazy links. */
public record DestinationSummary(
        UUID id,
        String name,
        String slug,
        ListingStatus status,
        UUID createdBy,
        UUID provinceId,
        UUID regionId,
        BigDecimal latitude,
        BigDecimal longitude,
        String coverKey,
        long viewCount,
        Instant publishedAt,
        Set<UUID> categoryIds,
        Set<UUID> interestIds,
        Set<UUID> tourismRegionIds
) {
    /** True for its province's zone or any of its tourism regions. */
    public boolean inRegion(UUID region) {
        return region.equals(regionId) || tourismRegionIds.contains(region);
    }

    public boolean published() {
        return status == ListingStatus.PUBLISHED;
    }
}
