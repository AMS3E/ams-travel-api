package ams.asmtravel.api.services.taxonomy.category.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String iconKey,
        Integer sortOrder,
        Boolean isActive,
        /** Who made it; null for the ones the seed wrote. */
        UUID createdBy,
        /** When it was made: what "Newest first" sorts on. */
        Instant createdAt
) {
}
