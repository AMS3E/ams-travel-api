package ams.asmtravel.api.services.corridor.domain;

import ams.asmtravel.api.services.destination.destination.domain.Destination;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Input for routing a corridor: stops in order with position and transport, plus their signature.
 * Legs with the same road mode are routed in one request; boats run alone; flights are off-road.
 */
public record RoutePlan(String signature, List<Waypoint> waypoints) {

    public RoutePlan {
        waypoints = List.copyOf(waypoints);
    }

    /** @param point null when the destination has no map position */
    public record Waypoint(String name, GeoPoint point, CorridorTransportType transport) {
    }

    public sealed interface Segment permits RoadRun, OffRoad {
    }

    /**
     * Legs routed in one request.
     *
     * @param firstStop index of the stop the first leg arrives at
     * @param points    the start stop, then each stop reached
     */
    public record RoadRun(int firstStop, RoadTravelMode mode, List<Waypoint> points) implements Segment {

        public List<GeoPoint> positions() {
            return points.stream().map(Waypoint::point).map(Objects::requireNonNull).toList();
        }

        /** True for a boat run: own pace, straight legs where there is no road. */
        public boolean ownPace() {
            return points.get(1).transport().ownPace();
        }
    }

    /** @param stop index of the stop the leg arrives at */
    public record OffRoad(int stop, Waypoint from, Waypoint to) implements Segment {
    }

    public static RoutePlan of(List<CorridorStop> stopsInOrder) {
        List<Waypoint> waypoints = stopsInOrder.stream().map(stop -> {
            Destination place = stop.getDestination();
            GeoPoint point = place.getLatitude() == null || place.getLongitude() == null
                    ? null
                    : new GeoPoint(place.getLatitude().doubleValue(), place.getLongitude().doubleValue());
            return new Waypoint(place.getName(), point, CorridorTransportType.orDefault(stop.getTransportType()));
        }).toList();
        return new RoutePlan(RouteSignature.of(stopsInOrder), waypoints);
    }

    /** Name of the first stop without a position; null if all have one. */
    public String firstUnplaced() {
        return waypoints.stream().filter(waypoint -> waypoint.point() == null).map(Waypoint::name).findFirst().orElse(null);
    }

    /**
     * All legs, grouped into routing requests.
     *
     * @param maxStopovers max intermediate points per request
     * @throws IllegalStateException if a stop has no position
     */
    public List<Segment> segments(int maxStopovers) {
        if (firstUnplaced() != null) {
            throw new IllegalStateException("Every stop needs a position before its legs are planned");
        }
        List<Segment> segments = new ArrayList<>();
        int stop = 1;
        while (stop < waypoints.size()) {
            Waypoint from = waypoints.get(stop - 1);
            Waypoint to = waypoints.get(stop);
            RoadTravelMode mode = to.transport().roadMode();
            if (mode == null || from.point().samePlaceAs(to.point())) {
                segments.add(new OffRoad(stop, from, to));
                stop += 1;
                continue;
            }
            int last = stop;
            while (last + 1 < waypoints.size()
                    && waypoints.get(last + 1).transport().roadMode() == mode
                    && waypoints.get(last + 1).transport().ownPace() == to.transport().ownPace()
                    && !waypoints.get(last).point().samePlaceAs(waypoints.get(last + 1).point())
                    && last + 1 - stop <= maxStopovers) {
                last += 1;
            }
            segments.add(new RoadRun(stop, mode, waypoints.subList(stop - 1, last + 1)));
            stop = last + 1;
        }
        return segments;
    }
}
