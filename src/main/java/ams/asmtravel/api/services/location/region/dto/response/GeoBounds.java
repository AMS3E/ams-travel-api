package ams.asmtravel.api.services.location.region.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * The box around a set of places, for a map to frame: the Map screen zooms to a
 * province or a region with it. Null where there is nothing published to frame.
 */
public record GeoBounds(BigDecimal north, BigDecimal south, BigDecimal east, BigDecimal west) {

    /** A place's position, as the catalogue stores it. */
    public record Point(BigDecimal latitude, BigDecimal longitude) {
        public boolean known() {
            return latitude != null && longitude != null;
        }
    }

    /** The box around these points, or null when none has a position. */
    public static GeoBounds around(Collection<Point> points) {
        BigDecimal north = null;
        BigDecimal south = null;
        BigDecimal east = null;
        BigDecimal west = null;
        for (Point point : points) {
            if (!point.known()) {
                continue;
            }
            north = north == null ? point.latitude() : north.max(point.latitude());
            south = south == null ? point.latitude() : south.min(point.latitude());
            east = east == null ? point.longitude() : east.max(point.longitude());
            west = west == null ? point.longitude() : west.min(point.longitude());
        }
        return north == null ? null : new GeoBounds(north, south, east, west);
    }

    public BigDecimal centreLatitude() {
        return north.add(south).divide(BigDecimal.valueOf(2), 7, RoundingMode.HALF_UP);
    }

    public BigDecimal centreLongitude() {
        return east.add(west).divide(BigDecimal.valueOf(2), 7, RoundingMode.HALF_UP);
    }
}
