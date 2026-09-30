package ams.asmtravel.api.services.history.history;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.location.province.domain.Province;
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

import java.util.Set;
import java.util.UUID;

import static ams.asmtravel.api.support.Callers.contributor;
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

/**
 * Periods, monarchs, styles and religions, and what puts a place among them:
 * its timeline ({@code /destination-histories}), the monarchs who shaped it
 * ({@code /destination-monarchs}), and the styles and religions on the
 * destination itself.
 */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class HeritageIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired TestCatalogue catalogue;

    private Province province;
    private Destination temple;
    private String period;
    private String monarch;
    private String style;
    private String religion;

    @BeforeEach
    void vocabulary() throws Exception {
        province = catalogue.province();
        temple = catalogue.published(province, "Angkor Wat", 13.4125, 103.8670);
        period = create("/api/v1/historical-coverage/periods", "{\"name\":\"Angkor Period T\",\"startYear\":802,\"endYear\":1431}");
        monarch = create("/api/v1/historical-coverage/monarchs", "{\"name\":\"Suryavarman II T\",\"title\":\"King\",\"reignStartYear\":1113,\"reignEndYear\":1150}");
        style = create("/api/v1/historical-coverage/styles", "{\"name\":\"Angkor Wat Style T\",\"periodId\":\"" + period + "\"}");
        religion = create("/api/v1/historical-coverage/religions", "{\"name\":\"Hinduism T\",\"iconKey\":\"lotus\"}");
    }

    @Test
    void aPlacesHeritageIsItsTimelineMonarchsStylesAndReligions() throws Exception {
        mvc.perform(post("/api/v1/destination-histories").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + temple.getId() + "\",\"historicalPeriodId\":\"" + period
                                + "\",\"title\":\"Built 1113 – 1150 CE\",\"eventYear\":1113}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.historicalPeriodName").value("Angkor Period T"));
        mvc.perform(put("/api/v1/destination-monarchs/" + temple.getId()).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monarchs\":[{\"monarchId\":\"" + monarch + "\",\"description\":\"Raised it to Vishnu\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Raised it to Vishnu"))
                .andExpect(jsonPath("$[0].name").value("Suryavarman II T"));
        mvc.perform(put("/api/v1/destinations/" + temple.getId()).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content(templeWith("\"architectureStyleIds\":[\"" + style + "\"],\"religionIds\":[\"" + religion + "\"]")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.architectureStyleIds[0]").value(style))
                .andExpect(jsonPath("$.religionIds[0]").value(religion));
        nextRequest();

        mvc.perform(get("/api/v1/destination-monarchs").param("destinationId", temple.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reignStartYear").value(1113));
        mvc.perform(get("/api/v1/destination-monarchs"))
                .andExpect(jsonPath("$[*].destinationId", hasItem(temple.getId().toString())));
        mvc.perform(get("/api/v1/destinations/" + temple.getId()))
                .andExpect(jsonPath("$.architectureStyleIds[0]").value(style))
                .andExpect(jsonPath("$.religionIds[0]").value(religion));
        mvc.perform(get("/api/v1/destination-histories").param("destinationId", temple.getId().toString()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Built 1113 – 1150 CE"));
        mvc.perform(get("/api/v1/historical-coverage/styles").param("periodId", period))
                .andExpect(jsonPath("$[0].id").value(style));
    }

    @Test
    void deletingAVocabularyEntryUntiesIt() throws Exception {
        mvc.perform(post("/api/v1/destination-histories").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + temple.getId() + "\",\"historicalPeriodId\":\"" + period + "\",\"title\":\"Built\"}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/v1/destination-monarchs/" + temple.getId()).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monarchs\":[{\"monarchId\":\"" + monarch + "\"}]}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/destinations/" + temple.getId()).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content(templeWith("\"architectureStyleIds\":[\"" + style + "\"],\"religionIds\":[\"" + religion + "\"]")))
                .andExpect(status().isOk());
        nextRequest();

        mvc.perform(delete("/api/v1/historical-coverage/monarchs/" + monarch).with(owner())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/historical-coverage/religions/" + religion).with(owner())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/historical-coverage/periods/" + period).with(owner())).andExpect(status().isNoContent());
        nextRequest();

        mvc.perform(get("/api/v1/destination-monarchs").param("destinationId", temple.getId().toString()))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/v1/destinations/" + temple.getId()))
                .andExpect(jsonPath("$.religionIds", hasSize(0)))
                .andExpect(jsonPath("$.architectureStyleIds", hasSize(1)));
        mvc.perform(get("/api/v1/destination-histories").param("destinationId", temple.getId().toString()))
                .andExpect(jsonPath("$[0].historicalPeriodId").value(nullValue()));
        mvc.perform(get("/api/v1/historical-coverage/styles/" + style)).andExpect(jsonPath("$.periodId").value(nullValue()));
        nextRequest();

        mvc.perform(delete("/api/v1/historical-coverage/styles/" + style).with(owner())).andExpect(status().isNoContent());
        nextRequest();
        mvc.perform(get("/api/v1/destinations/" + temple.getId())).andExpect(jsonPath("$.architectureStyleIds", hasSize(0)));

        mvc.perform(delete("/api/v1/destinations/" + temple.getId()).with(owner())).andExpect(status().isNoContent());
        nextRequest();
        mvc.perform(get("/api/v1/destination-histories").param("destinationId", temple.getId().toString()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/destination-monarchs").param("destinationId", temple.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void whoMayTouchHeritage() throws Exception {
        Destination draft = catalogue.place(province, "Hidden draft", 12.0, 104.0, ListingStatus.DRAFT, null, Set.of());
        RequestPostProcessor writer = contributor("history@desk.test");

        for (String path : new String[] {"/api/v1/destination-histories", "/api/v1/destination-monarchs"}) {
            mvc.perform(get(path).param("destinationId", draft.getId().toString())).andExpect(status().isNotFound());
            mvc.perform(get(path).param("destinationId", draft.getId().toString()).with(owner())).andExpect(status().isOk());
        }
        mvc.perform(put("/api/v1/destination-monarchs/" + temple.getId()).with(writer).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/destination-monarchs/" + temple.getId()).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monarchs\":[{\"monarchId\":\"" + UUID.randomUUID() + "\"}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/destinations/" + temple.getId()).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content(templeWith("\"religionIds\":[\"" + UUID.randomUUID() + "\"]")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/historical-coverage/periods").with(customer("who@visit.test")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mine\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/historical-coverage/periods").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"angkor period t\"}"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/historical-coverage/monarchs/" + monarch).with(editor())).andExpect(status().isForbidden());
    }

    /* ---------------------------------------------------------------------- */

    /** Historical coverage reads top to bottom: a capital goes where it is put, and the others close up around it. */
    @Test
    void historicalCoverageKeepsItsOrder() throws Exception {
        String coverage = "/api/v1/historical-coverage/regions";
        String region = create("/api/v1/regions", "{\"name\":\"Ancient Capitals T\",\"code\":\"ANCIENT-T\",\"kind\":\"TOURISM\"}");
        String ishanapura = create(coverage, capital(region, "Ishanapura", null));
        create(coverage, capital(region, "Hariharalaya", null));
        String yasodharapura = create(coverage, capital(region, "Yasodharapura", null));
        String mahendraparvata = create(coverage, capital(region, "Mahendraparvata", 1));
        nextRequest();
        mvc.perform(get(coverage).param("regionId", region))
                .andExpect(jsonPath("$[*].title", contains("Ishanapura", "Mahendraparvata", "Hariharalaya", "Yasodharapura")))
                .andExpect(jsonPath("$[*].sortOrder", contains(0, 1, 2, 3)));

        // Moved to the top, the list stays whole; an edit that leaves its place out keeps it.
        mvc.perform(put(coverage + "/" + mahendraparvata).with(editor()).contentType(MediaType.APPLICATION_JSON)
                .content(capital(region, "Mahendraparvata", 0))).andExpect(status().isOk());
        mvc.perform(put(coverage + "/" + yasodharapura).with(editor()).contentType(MediaType.APPLICATION_JSON)
                .content(capital(region, "Yasodharapura (Angkor)", null))).andExpect(status().isOk());
        nextRequest();
        mvc.perform(get(coverage).param("regionId", region))
                .andExpect(jsonPath("$[*].title", contains("Mahendraparvata", "Ishanapura", "Hariharalaya", "Yasodharapura (Angkor)")))
                .andExpect(jsonPath("$[*].sortOrder", contains(0, 1, 2, 3)));

        // Taken away, the ones after it move up.
        mvc.perform(delete(coverage + "/" + ishanapura).with(owner())).andExpect(status().isNoContent());
        nextRequest();
        mvc.perform(get(coverage).param("regionId", region))
                .andExpect(jsonPath("$[*].title", contains("Mahendraparvata", "Hariharalaya", "Yasodharapura (Angkor)")))
                .andExpect(jsonPath("$[*].sortOrder", contains(0, 1, 2)));
    }

    private static String capital(String region, String title, Integer place) {
        return "{\"regionId\":\"" + region + "\",\"title\":\"" + title + "\""
                + (place == null ? "" : ",\"sortOrder\":" + place) + "}";
    }

    /** The temple as the destination form sends it, with these heritage fields. */
    private String templeWith(String heritage) {
        return "{\"name\":\"Angkor Wat\",\"provinceId\":\"" + province.getId()
                + "\",\"latitude\":13.4125,\"longitude\":103.8670," + heritage + "}";
    }

    private String create(String path, String json) throws Exception {
        return JsonPath.read(mvc.perform(post(path).with(editor()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private void nextRequest() {
        entityManager.flush();
        entityManager.clear();
    }
}
