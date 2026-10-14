package ams.asmtravel.api.services.badge;

import ams.asmtravel.api.services.badge.domain.Badge;
import ams.asmtravel.api.services.badge.repository.BadgeRepository;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static ams.asmtravel.api.support.Callers.customer;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** "Top 5% of active members" under the badge, and the page a shared badge links to. */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class BadgeStandingIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired TestCatalogue catalogue;
    @Autowired SeasonService seasons;
    @Autowired BadgeRepository badges;

    private Province province;

    @BeforeEach
    void aSeasonIsActive() {
        province = catalogue.province();
        if (seasons.findCurrent().isEmpty()) {
            LocalDate today = seasons.today();
            seasons.create(new CreateSeasonRequest("Standing season " + today, SeasonType.GREEN_SEASON, today, today, true));
        }
        Badge browser = badges.findAll().stream().filter(it -> it.getCode().equals("BROWSER_SILVER")).findFirst().orElseThrow();
        browser.setThreshold(1);
        badges.saveAndFlush(browser);
    }

    @Test
    void membersAreRankedAmongTheSeasonsActiveOnesAndAnEarnedBadgeHasAPage() throws Exception {
        Destination museum = catalogue.published(province, "National Museum S", 11.5657, 104.9290);
        Destination palace = catalogue.published(province, "Royal Palace S", 11.5641, 104.9312);
        RequestPostProcessor keen = customer("keen@visit.test");
        RequestPostProcessor casual = customer("casual@visit.test");
        RequestPostProcessor idle = customer("idle@visit.test");

        mvc.perform(put("/api/v1/badges/me/explorations/" + museum.getId()).with(keen)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/badges/me/explorations/" + palace.getId()).with(keen)).andExpect(status().isOk());
        String earned = mvc.perform(put("/api/v1/badges/me/explorations/" + museum.getId()).with(casual))
                .andExpect(jsonPath("$.earned[0].code").value("BROWSER_SILVER"))
                .andReturn().getResponse().getContentAsString();
        nextRequest();

        // Two active members: the keener one is in the top half, the other at the bottom; a stranger has no rank.
        mvc.perform(get("/api/v1/badges/me/progress").with(keen)).andExpect(jsonPath("$.percentile").value(50));
        mvc.perform(get("/api/v1/badges/me/progress").with(casual)).andExpect(jsonPath("$.percentile").value(100));
        mvc.perform(get("/api/v1/badges/me/progress").with(idle)).andExpect(jsonPath("$.percentile").isEmpty());

        String userBadgeId = JsonPath.read(earned, "$.earned[0].id");
        String page = "http://localhost:8080/api/v1/badges/shared/" + userBadgeId + "/page";
        mvc.perform(get("/api/v1/badges/me/earned").with(casual))
                .andExpect(jsonPath("$[0].shareUrl").value(page));
        mvc.perform(get("/api/v1/badges/shared/" + userBadgeId))
                .andExpect(jsonPath("$.shareUrl").value(page));
        mvc.perform(get("/api/v1/badges/shared/" + userBadgeId + "/page"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("og:title")))
                .andExpect(content().string(containsString("Visitor casual earned the")))
                .andExpect(content().string(containsString(page)));
        mvc.perform(get("/api/v1/badges/shared/" + java.util.UUID.randomUUID() + "/page")).andExpect(status().isNotFound());
        // Progress without an unlock carries no link.
        mvc.perform(get("/api/v1/badges/me/progress").with(keen))
                .andExpect(jsonPath("$.badges[?(@.earnedAt == null)].shareUrl").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.nullValue())));
    }

    private void nextRequest() {
        entityManager.flush();
        entityManager.clear();
    }
}
