package ams.asmtravel.api.services.taxonomy.amenity.dto.response;

import ams.asmtravel.api.services.taxonomy.amenity.domain.AmenityLevel;

import java.time.Instant;
import java.util.UUID;

public record AmenityResponse(
        UUID id,
        String name,
        String slug,
        String groupName,
        String iconKey,
        AmenityLevel level,
        Integer sortOrder,
        Boolean isActive,
        /** Who made it; null for the ones the changelog wrote. */
        UUID createdBy,
        Instant createdAt
) {
}
