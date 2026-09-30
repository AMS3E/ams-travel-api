package ams.asmtravel.api.services.destination.destination.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * A circle around a destination's pin where its stays and food are listed.
 * The bounding box keeps queries plain (works on H2 too); {@link #kilometresTo} measures the real distance.
 */
public record NearbyArea(double latitude, double longitude, double radiusKm) {

    /** 50 km reaches the nearby town a day-tripper sleeps in. */
    public static final double LISTING_RADIUS_KM = 50;

    private static final double EARTH_RADIUS_KM = 6_371.0088;
    private static final double KM_PER_DEGREE_LATITUDE = 111.32;

    /** The listing area around a destination's pin; empty when it has none. */
    public static Optional<NearbyArea> around(Destination destination) {
        if (destination.getLatitude() == null || destination.getLongitude() == null) {
            return Optional.empty();
        }
        return Optional.of(new NearbyArea(destination.getLatitude().doubleValue(),
                destination.getLongitude().doubleValue(), LISTING_RADIUS_KM));
    }

    public BigDecimal south() {
        return BigDecimal.valueOf(latitude - latitudeSpan());
    }

    public BigDecimal north() {
        return BigDecimal.valueOf(latitude + latitudeSpan());
    }

    public BigDecimal west() {
        return BigDecimal.valueOf(longitude - longitudeSpan());
    }

    public BigDecimal east() {
        return BigDecimal.valueOf(longitude + longitudeSpan());
    }

    /** Haversine distance from the centre. */
    public double kilometresTo(BigDecimal toLatitude, BigDecimal toLongitude) {
        double dLatitude = Math.toRadians(toLatitude.doubleValue() - latitude);
        double dLongitude = Math.toRadians(toLongitude.doubleValue() - longitude);
        double a = Math.pow(Math.sin(dLatitude / 2), 2) + Math.cos(Math.toRadians(latitude))
                * Math.cos(Math.toRadians(toLatitude.doubleValue())) * Math.pow(Math.sin(dLongitude / 2), 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    private double latitudeSpan() {
        return radiusKm / KM_PER_DEGREE_LATITUDE;
    }

    private double longitudeSpan() {
        return radiusKm / (KM_PER_DEGREE_LATITUDE * Math.max(Math.cos(Math.toRadians(latitude)), 0.01));
    }
}
