package ams.asmtravel.api.services.corridor.domain;

/** A position on the map, in degrees. */
public record GeoPoint(double latitude, double longitude) {

    private static final double EARTH_RADIUS_KM = 6_371.0088;
    /** Closer than this counts as the same place. */
    private static final double SAME_PLACE_KM = 0.02;

    /** Great-circle distance, for a leg with no road under it. */
    public double kilometresTo(GeoPoint other) {
        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(other.latitude);
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(other.longitude - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    public boolean samePlaceAs(GeoPoint other) {
        return kilometresTo(other) < SAME_PLACE_KM;
    }
}
