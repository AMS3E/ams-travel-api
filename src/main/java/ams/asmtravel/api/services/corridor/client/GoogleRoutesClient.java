package ams.asmtravel.api.services.corridor.client;

import ams.asmtravel.api.services.corridor.config.GoogleMapsProperties;
import ams.asmtravel.api.services.corridor.domain.GeoPoint;
import ams.asmtravel.api.services.corridor.domain.RoadRoute;
import ams.asmtravel.api.services.corridor.domain.RoadTravelMode;
import ams.asmtravel.api.services.corridor.exception.GoogleRoutesException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Client for Google Routes {@code computeRoutes}: sends points in the given order, returns a {@link RoadRoute}.
 * Traffic is ignored (a plan, not a live trip; traffic tiers cost more).
 */
@Component
public class GoogleRoutesClient {

    /** Google allows 25 intermediates; past 10 a request is billed at the higher tier. */
    public static final int MAX_INTERMEDIATES = 10;

    /** Only the fields we read: Google bills by the fields asked for. */
    static final String FIELD_MASK = String.join(",",
            "routes.distanceMeters", "routes.duration", "routes.polyline.encodedPolyline",
            "routes.legs.distanceMeters", "routes.legs.duration", "routes.legs.polyline.encodedPolyline");

    private final GoogleMapsProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired
    public GoogleRoutesClient(GoogleMapsProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, RestClient.builder().requestFactory(requestFactory(properties)));
    }

    /** For tests: they bind a mock server to the builder. */
    GoogleRoutesClient(GoogleMapsProperties properties, ObjectMapper objectMapper, RestClient.Builder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = builder.build();
    }

    /** Whether a server key is set; without one nothing is sent. */
    public boolean isConfigured() {
        return properties.configured();
    }

    /**
     * The route through {@code points} (2 to {@link #MAX_INTERMEDIATES} + 2), in order; empty when there is none.
     *
     * @throws GoogleRoutesException when there is no key or Google fails
     */
    public Optional<RoadRoute> computeRoute(List<GeoPoint> points, RoadTravelMode mode) {
        if (points.size() < 2 || points.size() > MAX_INTERMEDIATES + 2) {
            throw new IllegalArgumentException("A route request takes 2 to " + (MAX_INTERMEDIATES + 2) + " points");
        }
        if (!isConfigured()) {
            throw new GoogleRoutesException(null, null, "No Google Maps server key is configured", null);
        }

        String body;
        try {
            body = objectMapper.writeValueAsString(requestFor(points, mode));
        } catch (JacksonException problem) {
            throw new IllegalStateException("Could not write the Google Routes request", problem);
        }

        String answer;
        try {
            answer = restClient.post()
                    .uri(properties.routesUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Goog-Api-Key", properties.apiKey())
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .body(body)
                    .exchange((request, response) -> {
                        String text = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        if (response.getStatusCode().isError()) {
                            throw failure(response.getStatusCode(), text);
                        }
                        return text;
                    });
        } catch (RestClientException problem) {
            throw new GoogleRoutesException(null, null, "Google Routes did not answer: " + problem.getMessage(), problem);
        }

        return readRoute(answer);
    }

    private static ComputeRoutesRequest requestFor(List<GeoPoint> points, RoadTravelMode mode) {
        boolean motorised = mode == RoadTravelMode.DRIVE || mode == RoadTravelMode.TWO_WHEELER;
        return new ComputeRoutesRequest(
                waypoint(points.getFirst()),
                waypoint(points.getLast()),
                points.subList(1, points.size() - 1).stream().map(GoogleRoutesClient::waypoint).toList(),
                mode.name(),
                // Google refuses a routing preference for walking and cycling.
                motorised ? "TRAFFIC_UNAWARE" : null,
                false,
                false,
                "HIGH_QUALITY",
                "ENCODED_POLYLINE",
                "en-US",
                "METRIC");
    }

    private static Waypoint waypoint(GeoPoint point) {
        return new Waypoint(new Location(new LatLng(point.latitude(), point.longitude())));
    }

    private Optional<RoadRoute> readRoute(String answer) {
        ComputeRoutesResponse response;
        try {
            response = answer == null || answer.isBlank()
                    ? new ComputeRoutesResponse(null)
                    : objectMapper.readValue(answer, ComputeRoutesResponse.class);
        } catch (JacksonException problem) {
            throw new GoogleRoutesException(HttpStatus.OK, null, "Google Routes answered with something unreadable", problem);
        }
        // No route at all comes back as an empty object, not as an error.
        if (response.routes() == null || response.routes().isEmpty()) {
            return Optional.empty();
        }
        Route route = response.routes().getFirst();
        List<RoadRoute.Leg> legs = route.legs() == null ? List.of() : route.legs().stream()
                .map(leg -> new RoadRoute.Leg(meters(leg.distanceMeters()), seconds(leg.duration()), line(leg.polyline())))
                .toList();
        return Optional.of(new RoadRoute(meters(route.distanceMeters()), seconds(route.duration()), line(route.polyline()), legs));
    }

    private GoogleRoutesException failure(HttpStatusCode status, String body) {
        String reason = null;
        String message = body;
        try {
            GoogleError error = objectMapper.readValue(body, GoogleError.class);
            if (error.error() != null) {
                reason = error.error().status();
                message = error.error().message();
            }
        } catch (JacksonException unreadable) {
            // Not Google's error shape (e.g. a proxy page): keep the raw text.
        }
        return new GoogleRoutesException(status, reason,
                "Google Routes answered " + status.value() + (message == null || message.isBlank() ? "" : ": " + message.strip()),
                null);
    }

    /** Proto3 JSON omits zero, so missing means 0. */
    private static long meters(Long value) {
        return value == null ? 0 : value;
    }

    /** Parses Google's duration, e.g. {@code "5025s"}. */
    static long seconds(String duration) {
        if (duration == null || duration.isBlank()) {
            return 0;
        }
        String number = duration.endsWith("s") ? duration.substring(0, duration.length() - 1) : duration;
        try {
            return Math.round(Double.parseDouble(number));
        } catch (NumberFormatException unreadable) {
            return 0;
        }
    }

    private static String line(Polyline polyline) {
        return polyline == null || polyline.encodedPolyline() == null ? "" : polyline.encodedPolyline();
    }

    private static JdkClientHttpRequestFactory requestFactory(GoogleMapsProperties properties) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(properties.timeout());
        return factory;
    }

    /* Google's JSON (only the parts we use). */

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    record ComputeRoutesRequest(
            Waypoint origin,
            Waypoint destination,
            List<Waypoint> intermediates,
            String travelMode,
            String routingPreference,
            boolean computeAlternativeRoutes,
            boolean optimizeWaypointOrder,
            String polylineQuality,
            String polylineEncoding,
            String languageCode,
            String units) {
    }

    record Waypoint(Location location) {
    }

    record Location(LatLng latLng) {
    }

    record LatLng(double latitude, double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ComputeRoutesResponse(List<Route> routes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Route(Long distanceMeters, String duration, Polyline polyline, List<RouteLeg> legs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RouteLeg(Long distanceMeters, String duration, Polyline polyline) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Polyline(String encodedPolyline) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GoogleError(ErrorBody error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorBody(Integer code, String message, String status) {
    }
}
