package ams.asmtravel.api.services.point;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.season.service.SeasonService;
import ams.asmtravel.api.support.TestCatalogue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ams.asmtravel.api.support.Callers.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The points badge is awarded after the rating commits, so this test commits for real
 * (no test transaction) and removes everything it made afterwards.
 */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
class PointBadgeTests {

    private static final String EMAIL = "badge.points@visit.test";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestCatalogue catalogue;
    @Autowired SeasonService seasonService;

    private final List<UUID> places = new ArrayList<>();
    private Province province;

    @Test
    void enoughSeasonPointsEarnThePointsBadgeOnce() throws Exception {
        Assumptions.assumeTrue(seasonService.findCurrent().isPresent(), "needs the seeded calendar to cover today");
        jdbc.update("update gamification.badges set threshold = 2 where code = 'POINTS_SILVER'");
        province = catalogue.province();
        RequestPostProcessor visitor = customer(EMAIL);
        for (String name : new String[] {"Badge Place A", "Badge Place B", "Badge Place C"}) {
            Destination place = catalogue.published(province, name, 13.5, 103.9);
            places.add(place.getId());
            mvc.perform(post("/api/v1/ratings").with(visitor).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetType\":\"DESTINATION\",\"targetId\":\"" + place.getId() + "\",\"stars\":5}"))
                    .andExpect(status().isCreated());
        }

        assertThat(jdbc.queryForObject("""
                select count(*) from gamification.user_badges ub
                join gamification.badges b on b.id = ub.badge_id
                join auth.users u on u.id = ub.user_id
                where b.code = 'POINTS_SILVER' and u.email = ? and ub.earned_at is not null
                """, Integer.class, EMAIL)).as("unlocked once, not once per point").isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                select ub.progress from gamification.user_badges ub
                join gamification.badges b on b.id = ub.badge_id
                join auth.users u on u.id = ub.user_id
                where b.code = 'POINTS_GOLDEN' and u.email = ? and ub.earned_at is null
                """, Integer.class, EMAIL)).as("progress towards the next level").isEqualTo(3);
        mvc.perform(get("/api/v1/badges/me/progress").with(visitor))
                .andExpect(jsonPath("$.tracks[?(@.type == 'POINTS')].level").value(contains("SILVER")))
                .andExpect(jsonPath("$.tracks[?(@.type == 'POINTS')].nextLevel").value(contains("GOLDEN")));
        mvc.perform(get("/api/v1/badges/me/earned").with(visitor))
                .andExpect(jsonPath("$[0].code").value("POINTS_SILVER"));
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("update gamification.badges set threshold = 30 where code = 'POINTS_SILVER'");
        String user = "(select id from auth.users where email = '" + EMAIL + "')";
        for (String table : new String[] {"gamification.user_badges", "gamification.point_transactions",
                "gamification.point_activities", "gamification.user_season_progress", "engagement.ratings"}) {
            jdbc.update("delete from " + table + " where user_id in " + user);
        }
        jdbc.update("delete from auth.users where email = ?", EMAIL);
        for (UUID place : places) {
            jdbc.update("delete from destination.destination_regions where destination_id = ?", place);
            jdbc.update("delete from destination.destinations where id = ?", place);
        }
        if (province != null) {
            jdbc.update("delete from taxonomy.provinces where id = ?", province.getId());
            jdbc.update("delete from taxonomy.regions where id = ?", province.getRegion().getId());
        }
    }
}
