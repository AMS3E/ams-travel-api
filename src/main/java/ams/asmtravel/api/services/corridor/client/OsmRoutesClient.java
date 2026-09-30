package ams.asmtravel.api.services.corridor.client;

import ams.asmtravel.api.services.corridor.config.OsmRoutingProperties;
import ams.asmtravel.api.services.corridor.domain.EncodedPolyline;
import ams.asmtravel.api.services.corridor.domain.GeoPoint;
import ams.asmtravel.api.services.corridor.domain.RoadRoute;
import ams.asmtravel.api.services.corridor.domain.RoadTravelMode;
import ams.asmtravel.api.services.corridor.exception.OsmRoutesException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Client for an OpenStreetMap routing server (OSRM), used when no Google key is set.
 * Step lines are joined into one Google-encoded line per leg, so stored legs look the same.
 */
@Component
public class OsmRoutesClient {

    /** The public server allows about one request a second. */
    private static final Duration GAP = Duration.ofSeconds(1);

    private final OsmRoutingProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final Object pacing = new Object();
    private long lastSentNanos;

    @Autowired
    public OsmRoutesClient(OsmRoutingProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, RestClient.builder().requestFactory(requestFactory(properties)));
    }

    /** For tests: they bind a mock server to the builder. */
    OsmRoutesClient(OsmRoutingProperties properties, ObjectMapper objectMapper, RestClient.Builder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = builder.build();
    }

    /** Whether a routing server is set; without one nothing is sent. */
    public boolean isConfigured() {
        return properties.configured();
    }

    /**
     * The route through {@code points}, in order; empty when there is no road.
     *
     * @throws OsmRoutesException when no server is set or the server fails
     */
    public Optional<RoadRoute> computeRoute(List<GeoPoint> points, RoadTravelMode mode) {
        if (points.size() < 2) {
            throw new IllegalArgumentException("A route request takes at least 2 points");
        }
        if (!isConfigured()) {
            throw new OsmRoutesException(null, "No OpenStreetMap routing server is configured", null);
        }

        pace();
        Answer answer;
        try {
            answer = restClient.get()
                    .uri(requestFor(points, mode))
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.USER_AGENT, properties.userAgent())
                    // No compression: the JDK client cannot unpack this server's deflate answer.
                    .header(HttpHeaders.ACCEPT_ENCODING, "identity")
                    .exchange((request, response) -> new Answer(response.getStatusCode(),
                            new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
        } catch (RestClientException problem) {
            throw new OsmRoutesException(null, "The OpenStreetMap routing server did not answer: " + problem.getMessage(), problem);
        }
        if (answer == null) {
            throw new OsmRoutesException(null, "The OpenStreetMap routing server did not answer", null);
        }
        return readRoute(answer);
    }

    /** {@code {url}/routed-car/route/v1/driving/lng,lat;lng,lat?…}: the profile path picks the means. */
    URI requestFor(List<GeoPoint> points, RoadTravelMode mode) {
        String coordinates = points.stream()
                .map(point -> String.format(Locale.ROOT, "%.6f,%.6f", point.longitude(), point.latitude()))
                .collect(Collectors.joining(";"));
        return URI.create(properties.url() + "/" + profile(mode) + "/route/v1/driving/" + coordinates
                + "?overview=false&steps=true&geometries=polyline&alternatives=false");
    }

    /** Waits so requests stay one {@link #GAP} apart, even in a startup burst. */
    private void pace() {
        synchronized (pacing) {
            long wait = lastSentNanos == 0 ? 0 : GAP.toNanos() - (System.nanoTime() - lastSentNanos);
            if (wait > 0) {
                try {
                    Thread.sleep(Duration.ofNanos(wait));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            lastSentNanos = System.nanoTime();
        }
    }

    static String profile(RoadTravelMode mode) {
        return switch (mode) {
            case DRIVE, TWO_WHEELER -> "routed-car";
            case BICYCLE -> "routed-bike";
            case WALK -> "routed-foot";
        };
    }

    private Optional<RoadRoute> readRoute(Answer answer) {
        OsrmResponse response;
        try {
            response = answer.body().isBlank() ? null : objectMapper.readValue(answer.body(), OsrmResponse.class);
        } catch (JacksonException unreadable) {
            response = null;
        }
        // OSRM says "no road" with an error status and a code of its own.
        if (response != null && ("NoRoute".equals(response.code()) || "NoSegment".equals(response.code()))) {
            return Optional.empty();
        }
        if (answer.status().isError() || response == null || !"Ok".equals(response.code())) {
            String said = response == null ? answer.body() : Objects.toString(response.message(), response.code());
            throw new OsmRoutesException(answer.status(), "The OpenStreetMap routing server answered " + answer.status().value()
                    + (said == null || said.isBlank() ? "" : ": " + said.strip()), null);
        }
        if (response.routes() == null || response.routes().isEmpty()) {
            return Optional.empty();
        }
        OsrmRoute route = response.routes().getFirst();
        List<RoadRoute.Leg> legs = route.legs() == null ? List.of() : route.legs().stream()
                .map(leg -> new RoadRoute.Leg(whole(leg.distance()), whole(leg.duration()), lineOf(leg)))
                .toList();
        String line = EncodedPolyline.join(legs.stream().map(RoadRoute.Leg::encodedPolyline).toList());
        return Optional.of(new RoadRoute(whole(route.distance()), whole(route.duration()), line, legs));
    }

    /** The leg's step lines joined end to end. */
    private static String lineOf(OsrmLeg leg) {
        if (leg.steps() == null) {
            return "";
        }
        return EncodedPolyline.join(leg.steps().stream()
                .map(OsrmStep::geometry)
                .filter(geometry -> geometry != null && !geometry.isEmpty())
                .toList());
    }

    private static long whole(Double value) {
        return value == null ? 0 : Math.round(value);
    }

    private static JdkClientHttpRequestFactory requestFactory(OsmRoutingProperties properties) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(properties.timeout());
        return factory;
    }

    private record Answer(HttpStatusCode status, String body) {
    }

    /* OSRM's JSON (only the parts we use). */

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmResponse(String code, String message, List<OsrmRoute> routes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmRoute(Double distance, Double duration, List<OsrmLeg> legs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmLeg(Double distance, Double duration, List<OsrmStep> steps) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmStep(String geometry) {
    }
}
