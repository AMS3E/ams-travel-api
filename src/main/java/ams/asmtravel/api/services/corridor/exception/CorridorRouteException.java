package ams.asmtravel.api.services.corridor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A corridor route could not be calculated; each message tells the curator what to do. Never includes the key. */
public class CorridorRouteException extends ResponseStatusException {

    private CorridorRouteException(HttpStatus status, String reason) {
        super(status, reason);
    }

    private CorridorRouteException(HttpStatus status, String reason, Throwable cause) {
        super(status, reason, cause);
    }

    public static CorridorRouteException notConfigured() {
        return new CorridorRouteException(HttpStatus.SERVICE_UNAVAILABLE,
                "Route calculation is off: set GOOGLE_MAPS_API_KEY, or OSM_ROUTING_URL, on the API to work roads out.");
    }

    public static CorridorRouteException tooFewStops() {
        return new CorridorRouteException(HttpStatus.UNPROCESSABLE_CONTENT, "Add at least two stops before working out a route.");
    }

    public static CorridorRouteException busy() {
        return new CorridorRouteException(HttpStatus.CONFLICT, "This route is being worked out already. Try again in a moment.");
    }

    public static CorridorRouteException unplaced(String destination) {
        return new CorridorRouteException(HttpStatus.UNPROCESSABLE_CONTENT, destination
                + " has no position on the map. Add its latitude and longitude on the destination, then work the route out again.");
    }

    public static CorridorRouteException noRoad(String from, String to) {
        return new CorridorRouteException(HttpStatus.UNPROCESSABLE_CONTENT, "No road was found from " + from + " to " + to
                + ". If that part of the trip is by water or air, set the stop's transport to Boat or Flight.");
    }

    public static CorridorRouteException unexpectedLegs(int received, int expected) {
        return new CorridorRouteException(HttpStatus.BAD_GATEWAY,
                "The routing service answered with " + received + " legs for " + expected + " stops.");
    }

    /** Maps a Google failure to a curator message. */
    public static CorridorRouteException from(GoogleRoutesException problem) {
        if (problem.googleStatus() == null) {
            return new CorridorRouteException(HttpStatus.SERVICE_UNAVAILABLE, "Google Routes could not be reached. Try again in a moment.");
        }
        return switch (problem.googleStatus().value()) {
            case 401, 403 -> new CorridorRouteException(HttpStatus.BAD_GATEWAY, "Google refused the server key (" + problem.getMessage()
                    + "). Check that the Routes API is enabled for GOOGLE_MAPS_API_KEY and that the key's restrictions allow this server.");
            case 429 -> new CorridorRouteException(HttpStatus.SERVICE_UNAVAILABLE, "Google's route quota is used up for now. Try again later.");
            case 400 -> new CorridorRouteException(HttpStatus.UNPROCESSABLE_CONTENT, "Google could not route these stops: " + problem.getMessage());
            default -> new CorridorRouteException(HttpStatus.BAD_GATEWAY, problem.getMessage());
        };
    }

    /** Maps an OSM failure to a curator message; the original stays as the cause. */
    public static CorridorRouteException from(OsmRoutesException problem) {
        if (problem.serverStatus() == null) {
            return new CorridorRouteException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The OpenStreetMap routing server could not be reached. Try again in a moment.", problem);
        }
        return switch (problem.serverStatus().value()) {
            case 429 -> new CorridorRouteException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The OpenStreetMap routing server is busy. Try again in a few minutes.", problem);
            case 400 -> new CorridorRouteException(HttpStatus.UNPROCESSABLE_CONTENT, "These stops could not be routed: " + problem.getMessage(), problem);
            default -> new CorridorRouteException(HttpStatus.BAD_GATEWAY, problem.getMessage(), problem);
        };
    }
}
