package ams.asmtravel.api.services.corridor.domain;

import java.util.List;

/** A routed road through the points in order, with one leg per pair. Duration ignores traffic. */
public record RoadRoute(long distanceMeters, long durationSeconds, String encodedPolyline, List<Leg> legs) {

    public RoadRoute {
        legs = List.copyOf(legs);
    }

    /** The road from one point to the next. */
    public record Leg(long distanceMeters, long durationSeconds, String encodedPolyline) {
    }
}
