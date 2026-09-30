package ams.asmtravel.api.services.review.rating;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static ams.asmtravel.api.support.Callers.customer;
import static ams.asmtravel.api.support.Callers.editor;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A rating's whole life: rate, read, change, delete, and what the place's average does meanwhile. */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class RatingIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired TestCatalogue catalogue;

    private Destination temple;

    @BeforeEach
    void place() {
        temple = catalogue.published(catalogue.province(), "Ta Som", 13.4650, 103.9130);
    }

    @Test
    void aRatingIsCreatedReadChangedAndDeleted() throws Exception {
        RequestPostProcessor anna = customer("anna@visit.test");
        RequestPostProcessor bora = customer("bora@visit.test");

        // Create: once per person per place, only a published place, one to five stars.
        rate(null, 4).andExpect(status().isUnauthorized());
        String annas = JsonPath.read(rate(anna, 4).andExpect(status().isCreated())
                .andExpect(jsonPath("$.stars").value(4))
                .andReturn().getResponse().getContentAsString(), "$.id");
        rate(anna, 2).andExpect(status().isConflict());
        rate(anna, 6).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/ratings").with(anna).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"DESTINATION\",\"targetId\":\"" + UUID.randomUUID() + "\",\"stars\":3}"))
                .andExpect(status().isNotFound());
        String boras = JsonPath.read(rate(bora, 2).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        nextRequest();
        summary().andExpect(jsonPath("$[0].average").value(3.0)).andExpect(jsonPath("$[0].count").value(2));

        // Read: anyone reads one rating; "me" is the caller's own.
        mvc.perform(get("/api/v1/ratings/" + annas)).andExpect(jsonPath("$.stars").value(4));
        mine(anna).andExpect(jsonPath("$.id").value(annas));
        mine(customer("nobody@visit.test")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/ratings/me").param("targetType", "DESTINATION").param("targetId", temple.getId().toString()))
                .andExpect(status().isUnauthorized());

        // Update: its author only.
        change(bora, annas, 1).andExpect(status().isForbidden());
        change(anna, annas, 5).andExpect(jsonPath("$.stars").value(5));
        nextRequest();
        summary().andExpect(jsonPath("$[0].average").value(3.5));

        // Delete: its author, or a moderator.
        mvc.perform(delete("/api/v1/ratings/" + annas).with(bora)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/ratings/" + boras).with(editor())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/ratings/" + annas).with(anna)).andExpect(status().isNoContent());
        nextRequest();
        summary().andExpect(jsonPath("$[0].average").value(nullValue())).andExpect(jsonPath("$[0].count").value(0));
        mvc.perform(get("/api/v1/ratings/" + annas)).andExpect(status().isNotFound());
    }

    @Test
    void summariesNeedATypeAndSomePlaces() throws Exception {
        mvc.perform(get("/api/v1/ratings/summary").param("targetIds", temple.getId().toString())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/ratings/summary").param("targetType", "DESTINATION")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/ratings/summary").param("targetType", "STAY").param("targetIds", UUID.randomUUID().toString()))
                .andExpect(jsonPath("$[0].targetType").value("STAY"))
                .andExpect(jsonPath("$[0].count").value(0));
    }

    private ResultActions rate(RequestPostProcessor caller, int stars) throws Exception {
        var request = post("/api/v1/ratings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetType\":\"DESTINATION\",\"targetId\":\"" + temple.getId() + "\",\"stars\":" + stars + "}");
        return mvc.perform(caller == null ? request : request.with(caller));
    }

    private ResultActions change(RequestPostProcessor caller, String id, int stars) throws Exception {
        return mvc.perform(put("/api/v1/ratings/" + id).with(caller).contentType(MediaType.APPLICATION_JSON)
                .content("{\"stars\":" + stars + "}"));
    }

    private ResultActions mine(RequestPostProcessor caller) throws Exception {
        return mvc.perform(get("/api/v1/ratings/me").with(caller)
                .param("targetType", "DESTINATION").param("targetId", temple.getId().toString()));
    }

    private ResultActions summary() throws Exception {
        return mvc.perform(get("/api/v1/ratings/summary").param("targetType", "DESTINATION").param("targetIds", temple.getId().toString()));
    }

    private void nextRequest() {
        entityManager.flush();
        entityManager.clear();
    }
}
