package ams.asmtravel.api.services.corridor.client;

import ams.asmtravel.api.services.corridor.config.GoogleMapsProperties;
import ams.asmtravel.api.services.corridor.domain.GeoPoint;
import ams.asmtravel.api.services.corridor.domain.RoadRoute;
import ams.asmtravel.api.services.corridor.domain.RoadTravelMode;
import ams.asmtravel.api.services.corridor.exception.GoogleRoutesException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** What goes to Google Routes and what comes back, against a stand-in for Google. */
class GoogleRoutesClientTests {

    private static final URI ROUTES = URI.create("https://routes.test/directions/v2:computeRoutes");
    private static final GeoPoint PHNOM_PENH = new GeoPoint(11.5686, 104.9319);
    private static final GeoPoint KAMPONG_CHAM = new GeoPoint(11.9925, 105.4638);
    private static final GeoPoint KRATIE = new GeoPoint(12.4744, 106.0119);

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer google = MockRestServiceServer.bindTo(builder).build();

    private GoogleRoutesClient client(String key) {
        return new GoogleRoutesClient(new GoogleMapsProperties(key, ROUTES, Duration.ofSeconds(2)),
                JsonMapper.builder().build(), builder);
    }

    @Test
    void theStopsGoInOrderAsOriginStopoversAndDestinationAndTheLegsComeBack() {
        GoogleRoutesClient client = client("server-key");
        google.expect(requestTo(ROUTES))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "server-key"))
                .andExpect(header("X-Goog-FieldMask", GoogleRoutesClient.FIELD_MASK))
                .andExpect(jsonPath("$.origin.location.latLng.latitude").value(11.5686))
                .andExpect(jsonPath("$.intermediates", hasSize(1)))
                .andExpect(jsonPath("$.intermediates[0].location.latLng.longitude").value(105.4638))
                .andExpect(jsonPath("$.destination.location.latLng.latitude").value(12.4744))
                .andExpect(jsonPath("$.travelMode").value("DRIVE"))
                .andExpect(jsonPath("$.routingPreference").value("TRAFFIC_UNAWARE"))
                .andExpect(jsonPath("$.optimizeWaypointOrder").value(false))
                .andExpect(jsonPath("$.computeAlternativeRoutes").value(false))
                .andRespond(withSuccess("""
                        {"routes": [{
                          "distanceMeters": 245300, "duration": "15840s", "polyline": {"encodedPolyline": "whole"},
                          "legs": [
                            {"distanceMeters": 123400, "duration": "7260.5s", "polyline": {"encodedPolyline": "first"}},
                            {"distanceMeters": 121900, "duration": "8580s", "polyline": {"encodedPolyline": "second"}, "steps": []}
                          ]
                        }]}
                        """, MediaType.APPLICATION_JSON));

        Optional<RoadRoute> route = client.computeRoute(List.of(PHNOM_PENH, KAMPONG_CHAM, KRATIE), RoadTravelMode.DRIVE);

        google.verify();
        assertThat(route).isPresent();
        assertThat(route.get().distanceMeters()).isEqualTo(245_300);
        assertThat(route.get().encodedPolyline()).isEqualTo("whole");
        assertThat(route.get().legs()).containsExactly(
                new RoadRoute.Leg(123_400, 7261, "first"),
                new RoadRoute.Leg(121_900, 8580, "second"));
    }

    /** Google refuses a routing preference on foot or by bicycle, and has no intermediates to hear about. */
    @Test
    void aWalkAskesForNoRoutingPreference() {
        GoogleRoutesClient client = client("server-key");
        google.expect(requestTo(ROUTES))
                .andExpect(jsonPath("$.travelMode").value("WALK"))
                .andExpect(jsonPath("$.routingPreference").doesNotExist())
                .andExpect(jsonPath("$.intermediates").doesNotExist())
                .andRespond(withSuccess("{\"routes\":[{\"legs\":[{\"duration\":\"0s\"}]}]}", MediaType.APPLICATION_JSON));

        RoadRoute route = client.computeRoute(List.of(PHNOM_PENH, PHNOM_PENH), RoadTravelMode.WALK).orElseThrow();

        google.verify();
        // Zero is left out of Google's JSON.
        assertThat(route.legs()).containsExactly(new RoadRoute.Leg(0, 0, ""));
    }

    @Test
    void noRouteComesBackAsNothingRatherThanAnError() {
        GoogleRoutesClient client = client("server-key");
        google.expect(requestTo(ROUTES)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(client.computeRoute(List.of(PHNOM_PENH, KRATIE), RoadTravelMode.TWO_WHEELER)).isEmpty();
    }

    @Test
    void googlesRefusalCarriesItsStatusAndReasonButNotTheKey() {
        GoogleRoutesClient client = client("server-key");
        google.expect(requestTo(ROUTES)).andRespond(withStatus(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"error": {"code": 403, "message": "Routes API has not been used in this project.", "status": "PERMISSION_DENIED"}}
                        """));

        assertThatThrownBy(() -> client.computeRoute(List.of(PHNOM_PENH, KRATIE), RoadTravelMode.DRIVE))
                .isInstanceOfSatisfying(GoogleRoutesException.class, problem -> {
                    assertThat(problem.googleStatus().value()).isEqualTo(403);
                    assertThat(problem.googleReason()).isEqualTo("PERMISSION_DENIED");
                    assertThat(problem.getMessage()).contains("Routes API has not been used").doesNotContain("server-key");
                });
    }

    @Test
    void withoutAKeyNothingIsSent() {
        GoogleRoutesClient client = client("");

        assertThat(client.isConfigured()).isFalse();
        assertThatThrownBy(() -> client.computeRoute(List.of(PHNOM_PENH, KRATIE), RoadTravelMode.DRIVE))
                .isInstanceOf(GoogleRoutesException.class);
        google.verify();
    }

    @Test
    void googlesDurationsAreSeconds() {
        assertThat(GoogleRoutesClient.seconds("5025s")).isEqualTo(5025);
        assertThat(GoogleRoutesClient.seconds("59.6s")).isEqualTo(60);
        assertThat(GoogleRoutesClient.seconds(null)).isZero();
    }
}
