package ams.asmtravel.api.services.corridor.client;

import ams.asmtravel.api.services.corridor.config.OsmRoutingProperties;
import ams.asmtravel.api.services.corridor.domain.EncodedPolyline;
import ams.asmtravel.api.services.corridor.domain.GeoPoint;
import ams.asmtravel.api.services.corridor.domain.RoadRoute;
import ams.asmtravel.api.services.corridor.domain.RoadTravelMode;
import ams.asmtravel.api.services.corridor.exception.OsmRoutesException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** What goes to an OpenStreetMap routing server and what comes back, against a stand-in for the server. */
class OsmRoutesClientTests {

    private static final String SERVER = "https://osrm.test";
    private static final GeoPoint PHNOM_PENH = new GeoPoint(11.5686, 104.9319);
    private static final GeoPoint PREK_KDAM = new GeoPoint(11.8000, 104.8000);
    private static final GeoPoint KAMPONG_CHAM = new GeoPoint(11.9925, 105.4638);
    private static final GeoPoint KRATIE = new GeoPoint(12.4744, 106.0119);

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer osrm = MockRestServiceServer.bindTo(builder).build();

    private OsmRoutesClient client(String url) {
        return new OsmRoutesClient(new OsmRoutingProperties(url, Duration.ofSeconds(2), null), JsonMapper.builder().build(), builder);
    }

    /** An encoded line, escaped for a JSON string: the encoding uses the backslash. */
    private static String line(GeoPoint... points) {
        return EncodedPolyline.encode(List.of(points)).replace("\\", "\\\\");
    }

    @Test
    void theStopsGoInOrderAsLongitudeLatitudeAndEachLegIsOneLine() {
        OsmRoutesClient client = client(SERVER + "/");
        osrm.expect(requestTo(startsWith(SERVER + "/routed-car/route/v1/driving/104.931900,11.568600;105.463800,11.992500;106.011900,12.474400?")))
                .andExpect(requestTo(containsString("steps=true")))
                .andExpect(requestTo(containsString("geometries=polyline")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.USER_AGENT, containsString("AMS-Travel")))
                // The public server's compressed answer breaks the JDK client: ask for it plain.
                .andExpect(header(HttpHeaders.ACCEPT_ENCODING, "identity"))
                .andRespond(withSuccess("""
                        {"code": "Ok", "routes": [{
                          "distance": 245300.4, "duration": 15840.6,
                          "legs": [
                            {"distance": 123400.2, "duration": 7260.5, "steps": [{"geometry": "%s"}, {"geometry": "%s"}, {"geometry": ""}]},
                            {"distance": 121900, "duration": 8580, "steps": [{"geometry": "%s"}]}
                          ]
                        }]}
                        """.formatted(line(PHNOM_PENH, PREK_KDAM), line(PREK_KDAM, KAMPONG_CHAM), line(KAMPONG_CHAM, KRATIE)),
                        MediaType.APPLICATION_JSON));

        RoadRoute route = client.computeRoute(List.of(PHNOM_PENH, KAMPONG_CHAM, KRATIE), RoadTravelMode.DRIVE).orElseThrow();

        osrm.verify();
        assertThat(route.distanceMeters()).isEqualTo(245_300);
        assertThat(route.durationSeconds()).isEqualTo(15_841);
        assertThat(route.legs()).hasSize(2);
        assertThat(route.legs().getFirst().distanceMeters()).isEqualTo(123_400);
        assertThat(route.legs().getFirst().durationSeconds()).isEqualTo(7_261);
        // The first leg's two steps are one line, the point they share kept once.
        assertThat(EncodedPolyline.decode(route.legs().getFirst().encodedPolyline())).containsExactly(PHNOM_PENH, PREK_KDAM, KAMPONG_CHAM);
        assertThat(EncodedPolyline.decode(route.encodedPolyline())).containsExactly(PHNOM_PENH, PREK_KDAM, KAMPONG_CHAM, KRATIE);
    }

    @Test
    void eachMeansHasItsOwnRoads() {
        assertThat(OsmRoutesClient.profile(RoadTravelMode.DRIVE)).isEqualTo("routed-car");
        assertThat(OsmRoutesClient.profile(RoadTravelMode.TWO_WHEELER)).isEqualTo("routed-car");
        assertThat(OsmRoutesClient.profile(RoadTravelMode.BICYCLE)).isEqualTo("routed-bike");
        assertThat(OsmRoutesClient.profile(RoadTravelMode.WALK)).isEqualTo("routed-foot");
    }

    @Test
    void noRoadComesBackAsNothingRatherThanAnError() {
        OsmRoutesClient client = client(SERVER);
        osrm.expect(requestTo(startsWith(SERVER + "/routed-foot/")))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\": \"NoRoute\", \"message\": \"Impossible route between points\"}"));

        assertThat(client.computeRoute(List.of(PHNOM_PENH, KRATIE), RoadTravelMode.WALK)).isEmpty();
    }

    @Test
    void aBusyServerCarriesItsStatus() {
        OsmRoutesClient client = client(SERVER);
        osrm.expect(requestTo(startsWith(SERVER))).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("Too Many Requests"));

        assertThatThrownBy(() -> client.computeRoute(List.of(PHNOM_PENH, KRATIE), RoadTravelMode.DRIVE))
                .isInstanceOfSatisfying(OsmRoutesException.class, problem -> {
                    assertThat(problem.serverStatus().value()).isEqualTo(429);
                    assertThat(problem.getMessage()).contains("429");
                });
    }

    @Test
    void withoutAServerNothingIsSent() {
        OsmRoutesClient client = client("  ");

        assertThat(client.isConfigured()).isFalse();
        assertThatThrownBy(() -> client.computeRoute(List.of(PHNOM_PENH, KRATIE), RoadTravelMode.DRIVE))
                .isInstanceOf(OsmRoutesException.class);
        osrm.verify();
    }
}
