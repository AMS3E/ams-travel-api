package ams.asmtravel.api.services.favorite;

import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.support.TestCatalogue;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static ams.asmtravel.api.support.Callers.customer;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The heart on a region card and a province card: saved like a place, listed under their own tabs. */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class FavoriteTargetsIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired TestCatalogue catalogue;

    @Test
    void regionsAndProvincesAreSavedLikePlaces() throws Exception {
        Province province = catalogue.province();
        UUID zone = province.getRegion().getId();
        RequestPostProcessor me = customer("hearts@visit.test");
        entityManager.flush();
        entityManager.clear();

        mvc.perform(post("/api/v1/favorites").with(me).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"PROVINCE\",\"targetId\":\"" + province.getId() + "\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/favorites").with(me).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"REGION\",\"targetId\":\"" + zone + "\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/favorites").with(me).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"PROVINCE\",\"targetId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/favorites").with(me))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].targetType", containsInAnyOrder("PROVINCE", "REGION")));
        mvc.perform(delete("/api/v1/favorites/PROVINCE/" + province.getId()).with(me)).andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/api/v1/favorites").with(me))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].targetType").value("REGION"));
    }
}
