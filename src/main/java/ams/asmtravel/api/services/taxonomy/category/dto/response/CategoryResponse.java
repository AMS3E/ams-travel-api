package ams.asmtravel.api.services.taxonomy.category.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String iconKey,
        /** Its uploaded icon picture's key, or null. */
        String coverKey,
        /** The icon picture's link, or null. */
        String coverUrl,
        Integer sortOrder,
        Boolean isActive,
        /** Who made it; null for the ones the seed wrote. */
        UUID createdBy,
        /** When it was made: what "Newest first" sorts on. */
        Instant createdAt,
        /** Its Khmer name; null until an editor writes it. */
        String nameKm
) {
}
