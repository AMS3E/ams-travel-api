package ams.asmtravel.api.services.corridor;

import ams.asmtravel.api.services.corridor.client.GoogleRoutesClient;
import ams.asmtravel.api.services.corridor.client.OsmRoutesClient;
import ams.asmtravel.api.services.corridor.domain.EncodedPolyline;
import ams.asmtravel.api.services.corridor.domain.GeoPoint;
import ams.asmtravel.api.services.corridor.domain.RoadRoute;
import ams.asmtravel.api.services.corridor.domain.RoadTravelMode;
import ams.asmtravel.api.services.corridor.exception.GoogleRoutesException;
import ams.asmtravel.api.services.corridor.exception.OsmRoutesException;
import ams.asmtravel.api.services.corridor.service.CorridorRouteService;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.support.TestCatalogue;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static ams.asmtravel.api.support.Callers.editor;
import static ams.asmtravel.api.support.Callers.owner;
import static ams.asmtravel.api.support.Callers.staff;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A corridor planned over days, and the road Google Routes draws between its
 * stops. Google is a mock: what matters here is what is asked of it, and how
 * seldom.
 *
 * <p>Not rolled back like most tests: the route is read and written in
 * transactions of its own once the itinerary is committed, so these tests
 * commit, and delete what they wrote.</p>
 */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
class CorridorIntegrationTests {

    private static final RequestPostProcessor CURATOR = staff("curator@corridor.test", "EDITOR", "PERM_CORRIDOR_CURATE");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestCatalogue catalogue;
    @Autowired CorridorRouteService routeService;
    @MockitoBean GoogleRoutesClient googleRoutes;
    /** Off unless a test turns it on, as when no routing server is set. */
    @MockitoBean OsmRoutesClient osmRoutes;

    private Province province;
    private Destination phnomPenh;
    private Destination kohDach;
    private Destination kampongCham;
    private Destination kratie;
    private Destination kampi;
    private final List<UUID> destinations = new ArrayList<>();

    @BeforeEach
    void mekong() {
        province = catalogue.province();
        phnomPenh = place("CT Phnom Penh Riverside", 11.5686, 104.9319, ListingStatus.PUBLISHED);
        kohDach = place("CT Koh Dach", 11.6289, 104.9622, ListingStatus.PUBLISHED);
        kampongCham = place("CT Kampong Cham Trail", 11.9925, 105.4638, ListingStatus.PUBLISHED);
        kratie = place("CT Koh Trong", 12.4744, 106.0119, ListingStatus.PUBLISHED);
        kampi = place("CT Kampi Dolphin Pool", 12.5583, 106.0333, ListingStatus.PUBLISHED);
        when(googleRoutes.isConfigured()).thenReturn(true);
        when(googleRoutes.computeRoute(anyList(), any())).thenAnswer(CorridorIntegrationTests::road);
    }

    @AfterEach
    void forget() {
        jdbc.update("delete from taxonomy.corridors where slug like 'ct-%'");
        jdbc.update("delete from taxonomy.interests where name like 'CT %'");
        for (UUID id : destinations) {
            jdbc.update("delete from destination.destinations where id = ?", id);
        }
        jdbc.update("delete from taxonomy.provinces where id = ?", province.getId());
        jdbc.update("delete from taxonomy.regions where id = ?", province.getRegion().getId());
        jdbc.update("delete from system.audit_logs");
        for (String table : new String[] {"auth.user_roles", "auth.auth_identities", "auth.user_preferences"}) {
            jdbc.update("delete from " + table + " where user_id in (select id from auth.users where email like '%@corridor.test' or email like '%@desk.test')");
        }
        jdbc.update("delete from auth.users where email like '%@corridor.test' or email like '%@desk.test'");
    }

    @Test
    void aCorridorIsPlannedDayByDayAndDrawnOverTheRoad() throws Exception {
        String id = createCorridor("ct-mekong-discovery");
        mvc.perform(get("/api/v1/corridors/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/corridors")).andExpect(jsonPath("$[*].id", not(hasItem(id))));
        mvc.perform(get("/api/v1/corridors").with(CURATOR)).andExpect(jsonPath("$[*].id", hasItem(id)));

        itinerary(id, mekongDiscovery("08:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days", hasSize(3)))
                .andExpect(jsonPath("$.days[0].stops[0].sequence").value(1))
                .andExpect(jsonPath("$.days[0].stops[0].arrivalTime").value(startsWith("08:00")))
                .andExpect(jsonPath("$.days[0].stops[0].transportType").value(nullValue()))
                .andExpect(jsonPath("$.days[0].stops[0].distanceFromPreviousKm").value(nullValue()))
                .andExpect(jsonPath("$.days[0].stops[0].destination.name").value("CT Phnom Penh Riverside"))
                .andExpect(jsonPath("$.days[0].stops[0].destination.latitude").value(11.5686))
                .andExpect(jsonPath("$.days[1].stops[1].sequence").value(4))
                .andExpect(jsonPath("$.days[1].stops[1].transportType").value("BOAT"))
                .andExpect(jsonPath("$.days[1].stops[1].routePolyline").value(notNullValue()))
                // A day that begins where the last one ended travels nothing to get there.
                .andExpect(jsonPath("$.days[2].stops[0].distanceFromPreviousKm").value(0.0))
                // By boat beside the road: the road's 12.79 km, at a boat's 25 km/h, plus 15 minutes to board.
                .andExpect(jsonPath("$.days[2].stops[1].distanceFromPreviousKm").value(12.79))
                .andExpect(jsonPath("$.days[2].stops[1].travelTimeFromPreviousMinutes").value(46))
                .andExpect(jsonPath("$.route.status").value("CURRENT"))
                .andExpect(jsonPath("$.route.configured").value(true))
                .andExpect(jsonPath("$.route.encodedPolyline").value(notNullValue()))
                .andExpect(jsonPath("$.route.totalDistanceKm").value(notNullValue()))
                .andExpect(jsonPath("$.corridor.durationDays").value(3))
                .andExpect(jsonPath("$.corridor.durationNights").value(2))
                .andExpect(jsonPath("$.corridor.stopCount").value(6))
                .andExpect(jsonPath("$.corridor.startDestinationId").value(phnomPenh.getId().toString()))
                .andExpect(jsonPath("$.corridor.endDestinationId").value(kampi.getId().toString()));

        // The road legs — Phnom Penh, Koh Dach, Kampong Cham — went to Google as one request, in order; each boat run on its own.
        verify(googleRoutes, times(3)).computeRoute(anyList(), any());
        verify(googleRoutes).computeRoute(argThat(points -> points.size() == 3
                && points.getFirst().equals(point(phnomPenh)) && points.getLast().equals(point(kampongCham))), eq(RoadTravelMode.DRIVE));

        mvc.perform(get("/api/v1/corridors/" + id + "/itinerary")).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/corridors/" + id + "/publish").with(CURATOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.publishedAt").value(notNullValue()));

        // A visitor reads the stored route; Google is not asked again.
        mvc.perform(get("/api/v1/corridors/" + id + "/itinerary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.status").value("CURRENT"));
        mvc.perform(get("/api/v1/corridors").param("slug", "ct-mekong-discovery"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].routeStatus").value("CURRENT"));
        verify(googleRoutes, times(3)).computeRoute(anyList(), any());

        mvc.perform(patch("/api/v1/corridors/" + id + "/unpublish").with(CURATOR)).andExpect(jsonPath("$.status").value("DRAFT"));
        mvc.perform(delete("/api/v1/corridors/" + id).with(CURATOR)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/corridors/" + id).with(owner())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/corridors/" + id).with(CURATOR)).andExpect(status().isNotFound());
    }

    @Test
    void googleIsAskedOnlyWhenALegChanges() throws Exception {
        String id = createCorridor("ct-legs");
        itinerary(id, mekongDiscovery("08:00")).andExpect(status().isOk());
        clearInvocations(googleRoutes);

        // New times and notes: the same roads.
        itinerary(id, mekongDiscovery("08:45")).andExpect(jsonPath("$.route.status").value("CURRENT"));
        verify(googleRoutes, never()).computeRoute(anyList(), any());

        // The first two stops swapped: new legs.
        itinerary(id, "[" + day(stop(kohDach, null), stop(phnomPenh, "CAR"), stop(kampongCham, "CAR")) + "]")
                .andExpect(jsonPath("$.route.status").value("CURRENT"))
                .andExpect(jsonPath("$.corridor.durationDays").value(1));
        verify(googleRoutes, times(1)).computeRoute(anyList(), any());

        // Asked for, it is worked out whether it fits or not.
        mvc.perform(post("/api/v1/corridors/" + id + "/route").with(CURATOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.calculatedAt").value(notNullValue()));
        verify(googleRoutes, times(2)).computeRoute(anyList(), any());
    }

    @Test
    void withNoRoutingAtAllTheItinerarySavesAndTheRouteWaits() throws Exception {
        when(googleRoutes.isConfigured()).thenReturn(false);
        String id = createCorridor("ct-no-key");

        itinerary(id, mekongDiscovery("08:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.status").value("MISSING"))
                .andExpect(jsonPath("$.route.configured").value(false))
                .andExpect(jsonPath("$.days[0].stops[1].distanceFromPreviousKm").value(nullValue()));
        mvc.perform(post("/api/v1/corridors/" + id + "/route").with(CURATOR))
                .andExpect(status().isServiceUnavailable());
        verify(googleRoutes, never()).computeRoute(anyList(), any());
        verify(osmRoutes, never()).computeRoute(anyList(), any());
    }

    /** No Google key: the OpenStreetMap routing server draws the same roads, and Google is never asked. */
    @Test
    void withoutAGoogleKeyOpenStreetMapWorksTheRoadsOut() throws Exception {
        when(googleRoutes.isConfigured()).thenReturn(false);
        when(osmRoutes.isConfigured()).thenReturn(true);
        when(osmRoutes.computeRoute(anyList(), any())).thenAnswer(CorridorIntegrationTests::road);
        String id = createCorridor("ct-open-roads");

        itinerary(id, mekongDiscovery("08:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.status").value("CURRENT"))
                .andExpect(jsonPath("$.route.configured").value(true))
                .andExpect(jsonPath("$.route.totalDistanceKm").value(notNullValue()))
                .andExpect(jsonPath("$.days[0].stops[1].distanceFromPreviousKm").value(notNullValue()))
                .andExpect(jsonPath("$.days[0].stops[1].routePolyline").value(notNullValue()));
        verify(osmRoutes, times(3)).computeRoute(anyList(), eq(RoadTravelMode.DRIVE));
        verify(googleRoutes, never()).computeRoute(anyList(), any());

        when(osmRoutes.computeRoute(anyList(), any())).thenThrow(new OsmRoutesException(null, "no answer", null));
        mvc.perform(post("/api/v1/corridors/" + id + "/route").with(CURATOR))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(containsString("OpenStreetMap routing server could not be reached")));
    }

    /** A boat where no road runs beside the water is drawn straight, and the rest of the route stands. */
    @Test
    void aBoatWithNoRoadBesideItIsDrawnStraight() throws Exception {
        when(googleRoutes.computeRoute(argThat(points -> points != null && points.contains(point(kampi))), any()))
                .thenReturn(Optional.empty());
        String id = createCorridor("ct-open-water");

        itinerary(id, mekongDiscovery("08:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.status").value("CURRENT"))
                .andExpect(jsonPath("$.days[1].stops[1].distanceFromPreviousKm").value(notNullValue()))
                // Koh Trong to Kampi: 9.61 km as the crow flies, at 25 km/h, plus 15 minutes to board.
                .andExpect(jsonPath("$.days[2].stops[1].distanceFromPreviousKm").value(9.61))
                .andExpect(jsonPath("$.days[2].stops[1].travelTimeFromPreviousMinutes").value(39));
    }

    /** While a corridor is planned, its map draws the roads between stops not saved yet; nothing is stored. */
    @Test
    void theRoadsOfAnUnsavedPlanArePreviewed() throws Exception {
        Destination unmapped = place("CT Unmapped Stupa", 0, 0, ListingStatus.PUBLISHED);
        jdbc.update("update destination.destinations set latitude = null, longitude = null where id = ?", unmapped.getId());
        String stops = "{\"stops\":["
                + "{\"destinationId\":\"" + phnomPenh.getId() + "\"},"
                + "{\"destinationId\":\"" + kohDach.getId() + "\",\"transportType\":\"CAR\"},"
                + "{\"destinationId\":\"" + kampongCham.getId() + "\",\"transportType\":\"TUK_TUK\"},"
                + "{\"destinationId\":\"" + unmapped.getId() + "\",\"transportType\":\"CAR\"},"
                + "{\"destinationId\":\"" + kratie.getId() + "\",\"transportType\":\"BOAT\"}]}";

        mvc.perform(post("/api/v1/corridors/route-preview").with(CURATOR).contentType(MediaType.APPLICATION_JSON).content(stops))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.legs", hasSize(4)))
                .andExpect(jsonPath("$.legs[0].fromDestinationId").value(phnomPenh.getId().toString()))
                .andExpect(jsonPath("$.legs[0].toDestinationId").value(kohDach.getId().toString()))
                .andExpect(jsonPath("$.legs[0].distanceKm").value(notNullValue()))
                .andExpect(jsonPath("$.legs[1].transportType").value("TUK_TUK"))
                .andExpect(jsonPath("$.legs[1].polyline").value(notNullValue()))
                // The stop with no pin empties the two legs that touch it, and says why.
                .andExpect(jsonPath("$.legs[2].distanceKm").value(nullValue()))
                .andExpect(jsonPath("$.legs[3].distanceKm").value(nullValue()))
                .andExpect(jsonPath("$.error").value(containsString("CT Unmapped Stupa has no position")));
        verify(googleRoutes, times(1)).computeRoute(anyList(), any());
        assertNoCorridorWritten();

        mvc.perform(post("/api/v1/corridors/route-preview").with(staff("writer@corridor.test", "CONTRIBUTOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(stops))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/corridors/route-preview").with(CURATOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stops\":[{\"destinationId\":\"" + phnomPenh.getId() + "\"},{\"destinationId\":\"" + UUID.randomUUID() + "\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("No such destination")));

        when(googleRoutes.isConfigured()).thenReturn(false);
        mvc.perform(post("/api/v1/corridors/route-preview").with(CURATOR).contentType(MediaType.APPLICATION_JSON).content(stops))
                .andExpect(jsonPath("$.configured").value(false))
                .andExpect(jsonPath("$.legs[0].distanceKm").value(nullValue()));
    }

    private void assertNoCorridorWritten() {
        Integer rows = jdbc.queryForObject("select count(*) from destination.corridor_destinations where destination_id = ?",
                Integer.class, phnomPenh.getId());
        assertThat(rows).isZero();
    }

    /** Saved and published with no key, a corridor gets its route when the API next starts with one. */
    @Test
    void aPublishedCorridorWithNoRouteGetsOneOnceAKeyIsSet() throws Exception {
        when(googleRoutes.isConfigured()).thenReturn(false);
        String id = createCorridor("ct-waiting");
        itinerary(id, mekongDiscovery("08:00")).andExpect(jsonPath("$.route.status").value("MISSING"));
        mvc.perform(patch("/api/v1/corridors/" + id + "/publish").with(CURATOR)).andExpect(jsonPath("$.routeStatus").value("MISSING"));
        mvc.perform(get("/api/v1/corridors/" + id + "/itinerary")).andExpect(jsonPath("$.route.status").value("MISSING"));
        verify(googleRoutes, never()).computeRoute(anyList(), any());

        when(googleRoutes.isConfigured()).thenReturn(true);
        routeService.calculateMissingRoutes();

        verify(googleRoutes, times(3)).computeRoute(anyList(), any());
        mvc.perform(get("/api/v1/corridors/" + id + "/itinerary")).andExpect(jsonPath("$.route.status").value("CURRENT"));
        routeService.calculateMissingRoutes();
        verify(googleRoutes, times(3)).computeRoute(anyList(), any());
    }

    @Test
    void aRouteThatCannotBeWorkedOutSaysWhy() throws Exception {
        when(googleRoutes.computeRoute(anyList(), any())).thenThrow(new GoogleRoutesException(
                HttpStatus.FORBIDDEN, "PERMISSION_DENIED", "Google Routes answered 403: API key not valid", null));
        String id = createCorridor("ct-refused");

        itinerary(id, mekongDiscovery("08:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.status").value("MISSING"))
                .andExpect(jsonPath("$.route.error").value(containsString("Google refused the server key")));
        mvc.perform(post("/api/v1/corridors/" + id + "/route").with(CURATOR)).andExpect(status().isBadGateway());

        Destination unmapped = place("CT Unmapped Pagoda", 0, 0, ListingStatus.PUBLISHED);
        jdbc.update("update destination.destinations set latitude = null, longitude = null where id = ?", unmapped.getId());
        itinerary(id, "[" + day(stop(phnomPenh, null), stop(unmapped, "CAR")) + "]").andExpect(status().isOk());
        mvc.perform(post("/api/v1/corridors/" + id + "/route").with(CURATOR))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value(containsString("CT Unmapped Pagoda has no position")));
    }

    @Test
    void aPublishedCorridorStopsOnlyAtPublishedPlaces() throws Exception {
        String id = createCorridor("ct-publish");
        Destination draft = place("CT Draft Temple", 12.0, 105.0, ListingStatus.DRAFT);

        itinerary(id, "[" + day(stop(phnomPenh, null), stop(draft, "CAR")) + "]").andExpect(status().isOk());
        mvc.perform(patch("/api/v1/corridors/" + id + "/publish").with(CURATOR))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("CT Draft Temple")));

        itinerary(id, "[" + day(stop(phnomPenh, null), stop(kohDach, "CAR")) + "]").andExpect(status().isOk());
        mvc.perform(patch("/api/v1/corridors/" + id + "/publish").with(CURATOR)).andExpect(status().isOk());
        itinerary(id, "[" + day(stop(phnomPenh, null), stop(draft, "CAR")) + "]").andExpect(status().isBadRequest());
        itinerary(id, "[{\"stops\":[{\"destinationId\":\"" + phnomPenh.getId()
                + "\",\"arrivalTime\":\"10:00\",\"departureTime\":\"09:00\"}]}]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("leaves before it arrives")));
        itinerary(id, "[" + day(stop(phnomPenh, null)) + "]", staff("writer@corridor.test", "CONTRIBUTOR"))
                .andExpect(status().isForbidden());
    }

    /** What a curator creates goes live at once; its itinerary can follow. */
    @Test
    void aCorridorGoesLiveBeforeItsItineraryIsPlanned() throws Exception {
        String id = createCorridor("ct-live-first");
        mvc.perform(get("/api/v1/corridors/" + id)).andExpect(status().isNotFound());

        mvc.perform(patch("/api/v1/corridors/" + id + "/publish").with(CURATOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.stopCount").value(0));
        mvc.perform(get("/api/v1/corridors").param("slug", "ct-live-first"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Up the Mekong, island by island"));

        itinerary(id, "[" + day(stop(phnomPenh, null), stop(kohDach, "CAR")) + "]").andExpect(status().isOk());
        mvc.perform(get("/api/v1/corridors/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stopCount").value(2));
    }

    /**
     * A corridor is listed under interests from the Tourism Corridors shelf: a
     * save that says nothing about them keeps them, and a deleted interest lets go.
     */
    @Test
    void aCorridorIsListedUnderItsInterests() throws Exception {
        String interest = JsonPath.read(mvc.perform(post("/api/v1/interests").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"CT Temple Trails\",\"groupName\":\"Tourism Corridors\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        String body = mvc.perform(post("/api/v1/corridors").with(CURATOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Temple trail\",\"slug\":\"ct-trail\",\"interestIds\":[\"" + interest + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interestIds", hasSize(1)))
                .andExpect(jsonPath("$.interestIds[0]").value(interest))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(put("/api/v1/corridors/" + id).with(CURATOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Temple trail\",\"slug\":\"ct-trail\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interestIds", hasSize(1)));

        mvc.perform(delete("/api/v1/interests/" + interest).with(owner())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/corridors/" + id).with(CURATOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interestIds", hasSize(0)));
    }

    /** What /corridor-destinations answered before days, it still answers. */
    @Test
    void theFlatRouteStillAnswers() throws Exception {
        String id = createCorridor("ct-flat");
        mvc.perform(put("/api/v1/corridor-destinations/" + id).with(CURATOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationIds\":[\"" + kohDach.getId() + "\",\"" + phnomPenh.getId() + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].destinationId").value(kohDach.getId().toString()))
                .andExpect(jsonPath("$[1].sortOrder").value(2));

        itinerary(id, mekongDiscovery("08:00")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/corridor-destinations").param("corridorId", id).with(CURATOR))
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[5].sortOrder").value(6))
                .andExpect(jsonPath("$[5].destinationId").value(kampi.getId().toString()));
        mvc.perform(get("/api/v1/corridor-destinations").param("corridorId", id)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(put("/api/v1/corridor-destinations/" + id).with(CURATOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationIds\":[\"" + kohDach.getId() + "\"]}"))
                .andExpect(status().isConflict());
    }

    @Test
    void aDestinationThatGoesOrMovesTakesTheRouteWithIt() throws Exception {
        String id = createCorridor("ct-changes");
        itinerary(id, "[" + day(stop(phnomPenh, null), stop(kohDach, "CAR"), stop(kampongCham, "CAR")) + "]")
                .andExpect(jsonPath("$.route.status").value("CURRENT"));
        clearInvocations(googleRoutes);

        mvc.perform(put("/api/v1/destinations/" + kohDach.getId()).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"CT Koh Dach\",\"provinceId\":\"" + province.getId() + "\",\"latitude\":11.64,\"longitude\":104.97}"))
                .andExpect(status().isOk());
        verify(googleRoutes, times(1)).computeRoute(argThat(points -> points.contains(new GeoPoint(11.64, 104.97))), eq(RoadTravelMode.DRIVE));

        mvc.perform(delete("/api/v1/destinations/" + kohDach.getId()).with(owner())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/corridors/" + id + "/itinerary").with(CURATOR))
                .andExpect(jsonPath("$.days[0].stops", hasSize(2)))
                .andExpect(jsonPath("$.days[0].stops[1].stopOrder").value(2))
                .andExpect(jsonPath("$.days[0].stops[1].destination.id").value(kampongCham.getId().toString()))
                .andExpect(jsonPath("$.route.status").value("CURRENT"));
        verify(googleRoutes, times(2)).computeRoute(anyList(), any());
    }

    /* ---------------------------------------------------------------------- */

    private String createCorridor(String slug) throws Exception {
        String body = mvc.perform(post("/api/v1/corridors").with(CURATOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"3-Day Mekong Discovery\",\"slug\":\"" + slug + "\","
                                + "\"shortDescription\":\"Up the Mekong\",\"description\":\"Up the Mekong, island by island\",\"estimatedBudgetMin\":180,\"estimatedBudgetMax\":320}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.routeStatus").value("NONE"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    /** Phnom Penh and Koh Dach by car; Kampong Cham by car, Koh Trong by boat; Koh Trong again, then Kampi by boat. */
    private String mekongDiscovery(String start) {
        return "[" + day("{\"destinationId\":\"" + phnomPenh.getId() + "\",\"arrivalTime\":\"" + start
                        + "\",\"departureTime\":\"09:30\",\"recommendedDurationMinutes\":90,\"notes\":\"Start " + start + "\"}",
                stop(kohDach, "CAR"))
                + "," + day(stop(kampongCham, "CAR"), stop(kratie, "BOAT"))
                + "," + day(stop(kratie, "BOAT"), stop(kampi, "BOAT")) + "]";
    }

    private static String day(String... stops) {
        return "{\"title\":\"A day\",\"startTime\":\"08:00\",\"stops\":[" + String.join(",", stops) + "]}";
    }

    private static String stop(Destination destination, String transport) {
        return "{\"destinationId\":\"" + destination.getId() + "\""
                + (transport == null ? "" : ",\"transportType\":\"" + transport + "\"") + "}";
    }

    private org.springframework.test.web.servlet.ResultActions itinerary(String id, String days) throws Exception {
        return itinerary(id, days, CURATOR);
    }

    private org.springframework.test.web.servlet.ResultActions itinerary(String id, String days, RequestPostProcessor caller) throws Exception {
        return mvc.perform(put("/api/v1/corridors/" + id + "/itinerary").with(caller)
                .contentType(MediaType.APPLICATION_JSON).content("{\"days\":" + days + "}"));
    }

    private Destination place(String name, double latitude, double longitude, ListingStatus status) {
        Destination destination = catalogue.place(province, name, latitude, longitude, status, null, Set.of());
        destinations.add(destination.getId());
        return destination;
    }

    private static GeoPoint point(Destination destination) {
        return new GeoPoint(destination.getLatitude().doubleValue(), destination.getLongitude().doubleValue());
    }

    /** Google, roughly: a road a third longer than the crow flies, at 50 km/h. */
    private static Optional<RoadRoute> road(InvocationOnMock call) {
        List<GeoPoint> points = call.getArgument(0);
        List<RoadRoute.Leg> legs = new ArrayList<>();
        long meters = 0;
        long seconds = 0;
        for (int index = 1; index < points.size(); index++) {
            long distance = Math.round(points.get(index - 1).kilometresTo(points.get(index)) * 1_330);
            long duration = Math.round(distance / 50_000.0 * 3_600);
            legs.add(new RoadRoute.Leg(distance, duration, EncodedPolyline.encode(List.of(points.get(index - 1), points.get(index)))));
            meters += distance;
            seconds += duration;
        }
        return Optional.of(new RoadRoute(meters, seconds, EncodedPolyline.encode(points), legs));
    }
}
