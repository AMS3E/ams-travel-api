package ams.asmtravel.api.services.admin;

import ams.asmtravel.api.support.TestCatalogue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static ams.asmtravel.api.support.Callers.editor;
import static ams.asmtravel.api.support.Callers.owner;
import static ams.asmtravel.api.support.Callers.staff;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The tiles every dashboard opens on, counted across every other service.
 *
 * <p>Two desks read this and see different things: full control sees the table
 * counts, a moderator only the reviews and the badge period. The counts are the
 * one place a number from each service has to line up, so a service that stops
 * answering shows here first.</p>
 */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class OverviewIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired TestCatalogue catalogue;

    @Test
    void fullControlCountsEveryTableTheDashboardShows() throws Exception {
        catalogue.published(catalogue.province(), "Overview Temple", 13.41, 103.86);
        /* Reading the tiles does not mirror the reader; signing in is what does. */
        mvc.perform(get("/api/v1/users/me").with(owner())).andExpect(status().isOk());

        mvc.perform(get("/api/v1/admin/overview").with(owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts.destinations").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.counts.regions").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.counts.provinces").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.counts.categories").value(notNullValue()))
                .andExpect(jsonPath("$.counts.tags").value(notNullValue()))
                .andExpect(jsonPath("$.counts.interests").value(notNullValue()))
                .andExpect(jsonPath("$.counts.corridors").value(notNullValue()))
                .andExpect(jsonPath("$.counts.stays").value(notNullValue()))
                .andExpect(jsonPath("$.counts.foods").value(notNullValue()))
                .andExpect(jsonPath("$.counts.activities").value(notNullValue()))
                .andExpect(jsonPath("$.counts.partners").value(notNullValue()))
                .andExpect(jsonPath("$.counts.users").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.counts.favorites").value(notNullValue()))
                .andExpect(jsonPath("$.counts.badges").value(greaterThanOrEqualTo(9)))
                .andExpect(jsonPath("$.counts.reviews").value(notNullValue()))
                .andExpect(jsonPath("$.reviews.total").value(notNullValue()))
                .andExpect(jsonPath("$.recentReviews").isArray());
    }

    /**
     * A partner has no table of its own: the count is the accounts holding the
     * role, mirrored from the token at sign-in. Signing one in has to move it.
     */
    @Test
    void thePartnerCountIsTheAccountsHoldingTheRole() throws Exception {
        int before = count();
        mvc.perform(get("/api/v1/users/me").with(staff("counted@partner.test", "PARTNER", "PERM_LISTING_EDIT_OWN")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/admin/overview").with(owner()))
                .andExpect(jsonPath("$.counts.partners").value(greaterThanOrEqualTo(before + 1)));
    }

    /** A moderator reads the reviews and the period, and is shown no table counts. */
    @Test
    void aModeratorSeesTheReviewsButNotTheCounts() throws Exception {
        mvc.perform(get("/api/v1/admin/overview").with(editor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counts").doesNotExist())
                .andExpect(jsonPath("$.reviews").exists())
                .andExpect(jsonPath("$.recentReviews").isArray());
    }

    @Test
    void theTilesAreTheseTwoDesksAndNobodyElses() throws Exception {
        mvc.perform(get("/api/v1/admin/overview")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/overview")
                        .with(staff("writer@desk.test", "CONTRIBUTOR", "PERM_DESTINATION_DRAFT_CREATE")))
                .andExpect(status().isForbidden());
    }

    /** How many of the latest reviews to show is bounded, so the tile cannot ask for the table. */
    @Test
    void theRecentCountIsBounded() throws Exception {
        mvc.perform(get("/api/v1/admin/overview").param("recent", "3").with(owner()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/overview").param("recent", "50").with(owner()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/overview").param("recent", "0").with(owner()))
                .andExpect(status().isBadRequest());
    }

    private int count() throws Exception {
        String body = mvc.perform(get("/api/v1/admin/overview").with(owner()))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.counts.partners");
    }
}
