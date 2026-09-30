package ams.asmtravel.api.services.corridor.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * The way from the previous stop, stored on the stop it arrives at.
 *
 * @param minutes  travel time only
 * @param polyline Google-encoded line
 */
public record CorridorLeg(BigDecimal distanceKm, Integer minutes, String polyline) {

    /** A leg along a routed road; timed by speed if routed for another mode or the transport has its own pace (boat). */
    public static CorridorLeg road(RoadRoute.Leg leg, CorridorTransportType transport, boolean timedBySpeed) {
        BigDecimal distance = kilometres(leg.distanceMeters() / 1000.0);
        int minutes = (timedBySpeed || transport.ownPace()) && transport.averageKmh() > 0
                ? minutesAt(distance.doubleValue(), transport.averageKmh()) + transport.boardingMinutes()
                : (int) Math.ceil(leg.durationSeconds() / 60.0);
        return new CorridorLeg(distance, minutes, leg.encodedPolyline());
    }

    /** A straight-line leg (crossing, flight, same place), timed by speed plus boarding. */
    public static CorridorLeg offRoad(GeoPoint from, GeoPoint to, CorridorTransportType transport) {
        String line = EncodedPolyline.encode(List.of(from, to));
        if (from.samePlaceAs(to)) {
            return new CorridorLeg(kilometres(0), 0, line);
        }
        double distance = from.kilometresTo(to);
        int speed = Math.max(1, transport.averageKmh());
        return new CorridorLeg(kilometres(distance), minutesAt(distance, speed) + transport.boardingMinutes(), line);
    }

    private static int minutesAt(double kilometres, int kmh) {
        return (int) Math.ceil(kilometres / kmh * 60);
    }

    private static BigDecimal kilometres(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
