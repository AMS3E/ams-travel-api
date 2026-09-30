package ams.asmtravel.api.services.corridor.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Google's encoded polyline format, precision 5.
 * Points are deltas, so lines cannot be glued as strings: decode, join, encode.
 */
public final class EncodedPolyline {

    private static final double PRECISION = 1e5;

    private EncodedPolyline() {
    }

    public static List<GeoPoint> decode(String encoded) {
        List<GeoPoint> points = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) {
            return points;
        }
        int index = 0;
        long latitude = 0;
        long longitude = 0;
        while (index < encoded.length()) {
            long[] next = readValue(encoded, index);
            latitude += next[0];
            index = (int) next[1];
            if (index >= encoded.length()) {
                break;
            }
            next = readValue(encoded, index);
            longitude += next[0];
            index = (int) next[1];
            points.add(new GeoPoint(latitude / PRECISION, longitude / PRECISION));
        }
        return points;
    }

    public static String encode(List<GeoPoint> points) {
        StringBuilder encoded = new StringBuilder();
        long previousLatitude = 0;
        long previousLongitude = 0;
        for (GeoPoint point : points) {
            long latitude = Math.round(point.latitude() * PRECISION);
            long longitude = Math.round(point.longitude() * PRECISION);
            writeValue(latitude - previousLatitude, encoded);
            writeValue(longitude - previousLongitude, encoded);
            previousLatitude = latitude;
            previousLongitude = longitude;
        }
        return encoded.toString();
    }

    /** Joins legs into one line; a shared point is kept once. */
    public static String join(List<String> legs) {
        List<GeoPoint> joined = new ArrayList<>();
        for (String leg : legs) {
            List<GeoPoint> points = decode(leg);
            if (!joined.isEmpty() && !points.isEmpty() && joined.getLast().equals(points.getFirst())) {
                points = points.subList(1, points.size());
            }
            joined.addAll(points);
        }
        return encode(joined);
    }

    /** Returns the value at {@code index} and the next index. */
    private static long[] readValue(String encoded, int index) {
        long result = 0;
        int shift = 0;
        int chunk;
        do {
            chunk = encoded.charAt(index++) - 63;
            result |= (long) (chunk & 0x1f) << shift;
            shift += 5;
        } while (chunk >= 0x20 && index < encoded.length());
        long value = (result & 1) != 0 ? ~(result >> 1) : result >> 1;
        return new long[]{value, index};
    }

    private static void writeValue(long value, StringBuilder out) {
        long shifted = value < 0 ? ~(value << 1) : value << 1;
        while (shifted >= 0x20) {
            out.append((char) ((0x20 | (shifted & 0x1f)) + 63));
            shifted >>= 5;
        }
        out.append((char) (shifted + 63));
    }
}
