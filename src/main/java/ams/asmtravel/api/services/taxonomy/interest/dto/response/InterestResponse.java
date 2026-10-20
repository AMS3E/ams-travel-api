package ams.asmtravel.api.services.taxonomy.interest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InterestResponse(
        UUID id,
        String name,
        String slug,
        String description,
        /** What the Discover layer lists it under, or null when it is not on those lists. */
        String groupName,
        String iconKey,
        /** Its uploaded picture's key, or null. */
        String coverKey,
        /** The picture's link, or null. */
        String coverUrl,
        Boolean isActive,
        /** Its place in its group, 0 for the first. */
        Integer sortOrder,
        /** Who made it; null for the ones the seed wrote. */
        UUID createdBy,
        /** When it was made: what "Newest first" sorts on. */
        Instant createdAt,
        /** Khmer name and description; null until an editor writes them. */
        String nameKm,
        String descriptionKm
) {
}
