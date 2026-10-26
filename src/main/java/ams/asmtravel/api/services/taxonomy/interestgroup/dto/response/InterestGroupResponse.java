package ams.asmtravel.api.services.taxonomy.interestgroup.dto.response;

import java.util.UUID;

public record InterestGroupResponse(
        UUID id,
        /** The name interests carry in their {@code groupName}. */
        String name,
        /** Its picture's key, or null. */
        String coverKey,
        /** The picture's link, or null. */
        String coverUrl,
        /** Its place in the row, 0 for the first. */
        Integer sortOrder
) {
}
