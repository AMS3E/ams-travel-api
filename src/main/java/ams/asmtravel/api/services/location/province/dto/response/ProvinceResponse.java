package ams.asmtravel.api.services.location.province.dto.response;

import ams.asmtravel.api.services.location.region.dto.response.GeoBounds;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * @param coverUrl      the picture on its card, or null
 * @param latitude      its centre: the marker on the Map screen
 * @param bounds        the box around its published places, for a map to frame; null with none
 * @param placeCount    published attractions, stays and food places in it
 * @param averageRating their ratings together, null when nobody rated yet
 * @param reviewCount   their published reviews together
 */
public record ProvinceResponse(
        UUID id,
        UUID regionId,
        String name,
        /** Its address on the website, made from its name. */
        String slug,
        String countryCode,
        Boolean isActive,
        String coverKey,
        String coverUrl,
        BigDecimal latitude,
        BigDecimal longitude,
        GeoBounds bounds,
        long placeCount,
        BigDecimal averageRating,
        long reviewCount,
        /** Who made it; null for the ones the seed wrote. */
        UUID createdBy,
        /** When it was made: what "Newest first" sorts on. */
        Instant createdAt
) {
}
