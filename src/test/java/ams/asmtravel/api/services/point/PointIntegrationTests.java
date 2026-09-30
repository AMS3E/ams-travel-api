package ams.asmtravel.api.services.point;

import ams.asmtravel.api.services.point.service.PointService;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.point.domain.PointSourceType;
import ams.asmtravel.api.services.season.domain.SeasonType;
import ams.asmtravel.api.services.season.dto.request.CreateSeasonRequest;
import ams.asmtravel.api.services.season.dto.response.SeasonResponse;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static ams.asmtravel.api.support.Callers.customer;
import static ams.asmtravel.api.support.Callers.editor;
import static ams.asmtravel.api.support.Callers.owner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** One point per person, place and day in the active season, shared by ratings and reviews. */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class PointIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestCatalogue catalogue;
    @Autowired PointService pointService;
    @Autowired SeasonService seasonService;

    private final RequestPostProcessor anna = customer("anna.points@visit.test");
    private Destination placeA;
    private Destination placeB;
    private Destination placeC;

    @BeforeEach
    void places() {
        Province province = catalogue.province();
        placeA = catalogue.published(province, "Point Place A", 13.41, 103.86);
        placeB = catalogue.published(province, "Point Place B", 13.42, 103.87);
        placeC = catalogue.published(province, "Point Place C", 13.43, 103.88);
        // The seeded calendar covers today until late 2027; after that the test brings its own season.
        if (seasonService.findCurrent().isEmpty()) {
            LocalDate today = seasonService.today();
            seasonService.create(new CreateSeasonRequest("Test season " + today, SeasonType.GREEN_SEASON, today.minusDays(1), today.plusDays(30), true));
        }
    }

    @Test
    void ratingAndReviewShareOnePointPerPlacePerDay() throws Exception {
        rate(anna, placeA, 4).andExpect(status().isCreated());
        assertThat(points(anna)).as("rates A").isEqualTo(1);

        String review = JsonPath.read(review(anna, placeA).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        assertThat(points(anna)).as("reviews A the same day").isEqualTo(1);

        String rating = JsonPath.read(mvc.perform(get("/api/v1/ratings/me").with(anna)
                        .param("targetType", "DESTINATION").param("targetId", placeA.getId().toString()))
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(put("/api/v1/ratings/" + rating).with(anna).contentType(MediaType.APPLICATION_JSON)
                .content("{\"stars\":5}")).andExpect(status().isOk());
        assertThat(points(anna)).as("changes the rating").isEqualTo(1);

        mvc.perform(put("/api/v1/reviews/" + review).with(anna).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Even better\"}")).andExpect(status().isOk());
        assertThat(points(anna)).as("changes the review").isEqualTo(1);

        review(anna, placeB).andExpect(status().isCreated());
        assertThat(points(anna)).as("reviews B").isEqualTo(2);

        rate(anna, placeC, 3).andExpect(status().isCreated());
        assertThat(points(anna)).as("rates C").isEqualTo(3);

        // Deleting and rating again the same day earns nothing more.
        mvc.perform(delete("/api/v1/ratings/" + rating).with(anna)).andExpect(status().isNoContent());
        rate(anna, placeA, 2).andExpect(status().isCreated());
        assertThat(points(anna)).as("deletes and rates A again").isEqualTo(3);

        // Another person earns their own point for the same place.
        RequestPostProcessor bora = customer("bora.points@visit.test");
        rate(bora, placeA, 5).andExpect(status().isCreated());
        assertThat(points(bora)).isEqualTo(1);

        mvc.perform(get("/api/v1/seasons/me/progress").with(anna))
                .andExpect(jsonPath("$.points").value(3))
                .andExpect(jsonPath("$.ratingCount").value(3))
                .andExpect(jsonPath("$.reviewCount").value(2))
                .andExpect(jsonPath("$.uniqueRatedTargetCount").value(2))
                .andExpect(jsonPath("$.uniqueReviewedTargetCount").value(2));
    }

    @Test
    void repeatsNeverAddASecondPointButTheNextDayMay() {
        UUID user = UUID.randomUUID();
        UUID place = placeA.getId();
        assertThat(pointService.awardDailyTargetPoint(user, "DESTINATION", place, PointSourceType.RATING, UUID.randomUUID())).isEqualTo(1);
        assertThat(pointService.awardDailyTargetPoint(user, "DESTINATION", place, PointSourceType.RATING, UUID.randomUUID())).isZero();
        assertThat(pointService.awardDailyTargetPoint(user, "DESTINATION", place, PointSourceType.REVIEW, UUID.randomUUID())).isZero();
        assertThat(jdbc.queryForObject("select count(*) from gamification.point_transactions where user_id = ?", Integer.class, user))
                .isEqualTo(1);

        // Yesterday's point for the same place is a different day: today's is still due for a new person.
        UUID other = UUID.randomUUID();
        UUID season = seasonService.findCurrent().orElseThrow().id();
        jdbc.update("""
                insert into gamification.point_transactions
                  (id, user_id, target_type, target_id, source_type, source_id, season_id, points, reward_date, created_at)
                values (?, ?, 'DESTINATION', ?, 'RATING', ?, ?, 1, ?, current_timestamp)
                """, UUID.randomUUID(), other, place, UUID.randomUUID(), season, seasonService.today().minusDays(1));
        assertThat(pointService.awardDailyTargetPoint(other, "DESTINATION", place, PointSourceType.RATING, UUID.randomUUID())).isEqualTo(1);
    }

    @Test
    void moderatingAReviewNeverEarnsAgain() throws Exception {
        String review = JsonPath.read(review(anna, placeA).andReturn().getResponse().getContentAsString(), "$.id");
        for (String status : new String[] {"HIDDEN", "PUBLISHED", "REJECTED", "PUBLISHED"}) {
            mvc.perform(put("/api/v1/reviews/" + review).with(editor()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"" + status + "\",\"moderationNote\":\"checked\"}"))
                    .andExpect(jsonPath("$.status").value(status))
                    .andExpect(jsonPath("$.moderationNote").value("checked"));
        }
        mvc.perform(put("/api/v1/reviews/" + review).with(editor()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DELETED\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/reviews/" + review).with(customer("someone.else@visit.test")).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Not mine\"}")).andExpect(status().isForbidden());

        // Deleted by its author, then written again the same day: back on the site, no new point.
        mvc.perform(delete("/api/v1/reviews/" + review).with(anna)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/reviews/" + review)).andExpect(status().isNotFound());
        review(anna, placeA).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(review));
        assertThat(points(anna)).isEqualTo(1);
        review(anna, placeA).andExpect(status().isConflict());
    }

    @Test
    void aNewSeasonStartsFromZeroAndKeepsTheOldOne() throws Exception {
        SeasonResponse before = seasonService.findCurrent().orElseThrow();
        rate(anna, placeA, 4);
        assertThat(points(anna)).isEqualTo(1);

        SeasonResponse next = seasonService.create(new CreateSeasonRequest("High season 2099", SeasonType.HIGH_SEASON,
                LocalDate.of(2099, 11, 1), LocalDate.of(2100, 4, 30), true));
        mvc.perform(post("/api/v1/seasons/" + next.id() + "/activate").with(customer("anna.points@visit.test")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/seasons/" + next.id() + "/activate").with(owner()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        nextRequest();
        mvc.perform(get("/api/v1/seasons/" + before.id())).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/v1/seasons/current")).andExpect(jsonPath("$.id").value(next.id().toString()));
        mvc.perform(delete("/api/v1/seasons/" + before.id()).with(owner())).andExpect(status().isConflict());

        mvc.perform(get("/api/v1/seasons/me/progress").with(anna)).andExpect(jsonPath("$.points").value(0));
        // The same place on the same day earns again: it is another season.
        review(anna, placeA).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/seasons/me/progress").with(anna)).andExpect(jsonPath("$.points").value(1));
        mvc.perform(get("/api/v1/seasons/me/history").with(anna))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.seasonId == '" + before.id() + "')].points").value(org.hamcrest.Matchers.contains(1)));
        assertThat(points(anna)).isEqualTo(2);

        mvc.perform(post("/api/v1/seasons/" + before.id() + "/activate").with(owner())).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/seasons/" + next.id() + "/complete").with(owner()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    private ResultActions rate(RequestPostProcessor caller, Destination place, int stars) throws Exception {
        return mvc.perform(post("/api/v1/ratings").with(caller).contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"DESTINATION\",\"targetId\":\"" + place.getId() + "\",\"stars\":" + stars + "}"));
    }

    private ResultActions review(RequestPostProcessor caller, Destination place) throws Exception {
        return mvc.perform(post("/api/v1/reviews").with(caller).contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"DESTINATION\",\"targetId\":\"" + place.getId()
                        + "\",\"title\":\"Worth it\",\"content\":\"Quiet at sunrise.\"}"));
    }

    /** The caller's points across every season. */
    private int points(RequestPostProcessor caller) throws Exception {
        nextRequest();
        String page = mvc.perform(get("/api/v1/points/me").with(caller)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.<Integer>read(page, "$.page.totalElements");
    }

    private void nextRequest() {
        entityManager.flush();
        entityManager.clear();
    }
}
