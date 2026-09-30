package ams.asmtravel.api.services.location.province.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ProvinceResponse(
        UUID id,
        UUID regionId,
        String name,
        /** Its address on the website, made from its name. */
        String slug,
        String countryCode,
        Boolean isActive,
        /** Who made it; null for the ones the seed wrote. */
        UUID createdBy,
        /** When it was made: what "Newest first" sorts on. */
        Instant createdAt
) {
}
