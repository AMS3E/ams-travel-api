package ams.asmtravel.api.services.location.province.domain;

import ams.asmtravel.api.services.location.region.dto.response.GeoBounds;

import java.math.BigDecimal;

/**
 * What the published places of a province add up to, for the second line of
 * its card: how many there are, their ratings and reviews together, and the box
 * around them for a map to frame.
 *
 * @param placeCount    published attractions, stays and food places in it
 * @param averageRating their star ratings averaged, weighted by how many each has; null when nobody rated
 * @param reviewCount   their published reviews together
 * @param bounds        the box around them; null when none has a pin
 */
public record ProvinceStanding(long placeCount, BigDecimal averageRating, long reviewCount, GeoBounds bounds) {
    public static final ProvinceStanding NONE = new ProvinceStanding(0, null, 0, null);
}
