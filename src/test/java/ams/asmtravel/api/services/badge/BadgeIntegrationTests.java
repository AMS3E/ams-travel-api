package ams.asmtravel.api.services.badge;

import ams.asmtravel.api.services.badge.domain.Badge;
import ams.asmtravel.api.services.badge.repository.BadgeRepository;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.season.domain.SeasonType;
import ams.asmtravel.api.services.season.dto.request.CreateSeasonRequest;
import ams.asmtravel.api.services.season.service.SeasonService;
import ams.asmtravel.api.support.TestCatalogue;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static ams.asmtravel.api.support.Callers.customer;
import static ams.asmtravel.api.support.Callers.editor;
import static ams.asmtravel.api.support.Callers.owner;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Badges per season: stamps, browses and collections unlock them; each season keeps its own. */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class BadgeIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired TestCatalogue catalogue;
    @Autowired SeasonService seasons;
    @Autowired BadgeRepository badges;

    private Province province;

    /** Whenever the suite runs, some season is active: the seeded calendar ends in 2027. */
    @BeforeEach
    void aSeasonIsActive() {
        province = catalogue.province();
        if (seasons.findCurrent().isEmpty()) {
            LocalDate today = seasons.today();
            seasons.create(new CreateSeasonRequest("Test season " + today, SeasonType.GREEN_SEASON, today, today, true));
        }
    }

    @Test
    void theSeasonCalendarRefusesOverlapsAndKeepsWhatRan() throws Exception {
        String id = JsonPath.read(mvc.perform(post("/api/v1/seasons").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"High season 2031/32\",\"type\":\"HIGH_SEASON\",\"startsOn\":\"2031-11-01\",\"endsOn\":\"2032-04-30\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.status").value("UPCOMING"))
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/api/v1/seasons").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Clash\",\"type\":\"GREEN_SEASON\",\"startsOn\":\"2032-04-01\",\"endsOn\":\"2032-06-30\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/seasons").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Green 2032\",\"type\":\"GREEN_SEASON\",\"startsOn\":\"2032-05-01\",\"endsOn\":\"2032-10-31\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/seasons/" + id).with(owner())).andExpect(status().isNoContent());

        String current = seasons.findCurrent().orElseThrow().id().toString();
        mvc.perform(get("/api/v1/seasons/current")).andExpect(jsonPath("$.id").value(current));
        mvc.perform(delete("/api/v1/seasons/" + current).with(owner())).andExpect(status().isConflict());
    }

    @Test
    void aStampOnSiteCountsOnceADayAndCanEarnABadge() throws Exception {
        threshold("ADVENTURE_SILVER", 1);
        Destination temple = catalogue.published(province, "Ta Som", 13.4618, 103.9129);
        RequestPostProcessor me = customer("stamper@visit.test");

        mvc.perform(post("/api/v1/stamps").with(me).contentType(MediaType.APPLICATION_JSON)
                        .content(checkIn(temple, 11.5564, 104.9282)))
                .andExpect(jsonPath("$.outcome").value("TOO_FAR"))
                .andExpect(jsonPath("$.badgesEarned", hasSize(0)));
        String stamp = mvc.perform(post("/api/v1/stamps").with(me).contentType(MediaType.APPLICATION_JSON)
                        .content(checkIn(temple, 13.4620, 103.9131)))
                .andExpect(jsonPath("$.outcome").value("COLLECTED"))
                .andExpect(jsonPath("$.badgesEarned", hasSize(1)))
                .andExpect(jsonPath("$.badgesEarned[0].code").value("ADVENTURE_SILVER"))
                .andExpect(jsonPath("$.badgesEarned[0].level").value("SILVER"))
                .andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/v1/stamps").with(me).contentType(MediaType.APPLICATION_JSON)
                        .content(checkIn(temple, 13.4620, 103.9131)))
                .andExpect(jsonPath("$.outcome").value("ALREADY_COLLECTED"))
                .andExpect(jsonPath("$.badgesEarned", hasSize(0)));

        String current = seasons.findCurrent().orElseThrow().name();
        mvc.perform(get("/api/v1/badges/me/progress").with(me))
                .andExpect(jsonPath("$.seasonName").value(current))
                .andExpect(jsonPath("$.tracks", hasSize(4)))
                .andExpect(jsonPath("$.tracks[?(@.type == 'STAMPS')].count").value(contains(1)))
                .andExpect(jsonPath("$.tracks[?(@.type == 'STAMPS')].level").value(contains("SILVER")))
                .andExpect(jsonPath("$.tracks[?(@.type == 'STAMPS')].nextLevel").value(contains("GOLDEN")))
                .andExpect(jsonPath("$.tracks[?(@.type == 'BROWSES')].level").value(contains(nullValue())))
                .andExpect(jsonPath("$.badges[?(@.code == 'ADVENTURE_SILVER')].progress").value(hasItem(1)));
        mvc.perform(get("/api/v1/badges/me/earned").with(me))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].seasonName").value(current))
                .andExpect(jsonPath("$[0].holderName").value(nullValue()));

        String shareId = JsonPath.read(stamp, "$.badgesEarned[0].id");
        mvc.perform(get("/api/v1/badges/shared/" + shareId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("SILVER"))
                .andExpect(jsonPath("$.type").value("STAMPS"))
                .andExpect(jsonPath("$.holderName").value("Visitor stamper"));

        mvc.perform(get("/api/v1/notifications").with(me))
                .andExpect(jsonPath("$[0].type").value("BADGE_EARNED"))
                .andExpect(jsonPath("$[0].data.userBadgeId").value(shareId));
    }

    @Test
    void aBrowseCountsOncePerPlaceADay() throws Exception {
        threshold("BROWSER_SILVER", 1);
        Destination museum = catalogue.published(province, "National Museum", 11.5657, 104.9290);
        Destination draft = catalogue.place(province, "Unfinished", 11.0, 104.0, ListingStatus.DRAFT, null, Set.of());
        RequestPostProcessor me = customer("browser@visit.test");

        mvc.perform(put("/api/v1/badges/me/explorations/" + museum.getId()).with(me))
                .andExpect(jsonPath("$.browseCounted").value(true))
                .andExpect(jsonPath("$.earned[0].code").value("BROWSER_SILVER"))
                .andExpect(jsonPath("$.tracks[?(@.type == 'BROWSES')].count").value(contains(1)));
        mvc.perform(put("/api/v1/badges/me/explorations/" + museum.getId()).with(me))
                .andExpect(jsonPath("$.browseCounted").value(false))
                .andExpect(jsonPath("$.earned", hasSize(0)))
                .andExpect(jsonPath("$.tracks[?(@.type == 'BROWSES')].count").value(contains(1)));
        mvc.perform(put("/api/v1/badges/me/explorations/" + draft.getId()).with(me)).andExpect(status().isNotFound());

        nextRequest();
        mvc.perform(get("/api/v1/destinations/" + museum.getId())).andExpect(jsonPath("$.viewCount").value(1));
    }

    @Test
    void aCollectionBadgeIsEarnedByStampingItsPlaces() throws Exception {
        Destination first = catalogue.published(province, "Preah Khan", 13.4623, 103.8716);
        Destination second = catalogue.published(province, "Neak Pean", 13.4617, 103.8952);
        mvc.perform(post("/api/v1/badges").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"lonely\",\"name\":\"Lonely\",\"type\":\"DESTINATION_STAMPS\",\"threshold\":1}"))
                .andExpect(status().isBadRequest());
        String badge = JsonPath.read(mvc.perform(post("/api/v1/badges").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"jayavarman_trail\",\"name\":\"Jayavarman trail\",\"type\":\"DESTINATION_STAMPS\","
                                + "\"threshold\":2,\"destinationIds\":[\"" + first.getId() + "\",\"" + second.getId() + "\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("JAYAVARMAN_TRAIL"))
                .andExpect(jsonPath("$.level").value(nullValue()))
                .andExpect(jsonPath("$.destinationIds", hasSize(2)))
                .andReturn().getResponse().getContentAsString(), "$.id");

        RequestPostProcessor me = customer("trail@visit.test");
        mvc.perform(post("/api/v1/stamps").with(me).contentType(MediaType.APPLICATION_JSON)
                        .content(checkIn(first, 13.4623, 103.8716)))
                .andExpect(jsonPath("$.badgesEarned", hasSize(0)));
        mvc.perform(post("/api/v1/stamps").with(me).contentType(MediaType.APPLICATION_JSON)
                        .content(checkIn(second, 13.4617, 103.8952)))
                .andExpect(jsonPath("$.badgesEarned[0].code").value("JAYAVARMAN_TRAIL"));

        // Earned, so it may not be deleted any more; it can be made inactive instead.
        mvc.perform(delete("/api/v1/badges/" + badge).with(owner())).andExpect(status().isConflict());
        mvc.perform(put("/api/v1/badges/" + badge).with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"JAYAVARMAN_TRAIL\",\"name\":\"Jayavarman trail\",\"type\":\"DESTINATION_STAMPS\","
                                + "\"threshold\":2,\"status\":\"INACTIVE\",\"destinationIds\":[\"" + first.getId() + "\"]}"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
        mvc.perform(get("/api/v1/badges/me/earned").with(me)).andExpect(jsonPath("$[0].code").value("JAYAVARMAN_TRAIL"));
    }

    @Test
    void theRulesAroundBadges() throws Exception {
        mvc.perform(get("/api/v1/badges/me/progress")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/badges/me/explorations/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/badges/me/earned")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/badges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize((int) badges.count())))
                .andExpect(jsonPath("$[?(@.code == 'REVIEWER_GOLDEN')].level").value(contains("GOLDEN")));
        mvc.perform(get("/api/v1/badges").param("type", "POINTS")).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(post("/api/v1/badges").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X\",\"name\":\"X\",\"type\":\"BROWSES\",\"level\":\"SILVER\",\"threshold\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/badges").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"NO_LEVEL\",\"name\":\"X\",\"type\":\"BROWSES\",\"threshold\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/badges/shared/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    private static String checkIn(Destination place, double latitude, double longitude) {
        return "{\"destinationId\":\"" + place.getId() + "\",\"latitude\":" + latitude + ",\"longitude\":" + longitude + "}";
    }

    private void threshold(String code, int value) {
        Badge badge = badges.findAll().stream().filter(it -> it.getCode().equals(code)).findFirst().orElseThrow();
        badge.setThreshold(value);
        badges.saveAndFlush(badge);
    }

    private void nextRequest() {
        entityManager.flush();
        entityManager.clear();
    }
}
