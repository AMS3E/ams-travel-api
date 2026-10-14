package ams.asmtravel.api.services.activity.popular;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.review.rating.domain.Rating;
import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;
import ams.asmtravel.api.support.TestCatalogue;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static ams.asmtravel.api.support.Callers.customer;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The Popular row is never empty on a fresh server: browses first, then the best rated, then the
 * catalogue A to Z, each row saying why it is there. Stays, food and corridors have lists of their own.
 */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class PopularListsIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired TestCatalogue catalogue;

    @Test
    void theRowIsFilledWhenBrowsesRunOut() throws Exception {
        Province province = catalogue.province();
        Province elsewhere = catalogue.province();
        Destination angkor = catalogue.published(province, "Angkor P", 13.4125, 103.8670);
        Destination bayon = catalogue.published(province, "Bayon P", 13.4413, 103.8590);
        Destination tonle = catalogue.published(province, "Tonle P", 13.0000, 104.0000);
        catalogue.published(elsewhere, "Far Away P", 10.6000, 104.1000);
        entityManager.persist(new Rating(UUID.randomUUID(), RatingTargetType.DESTINATION, tonle.getId(), 5));
        browse(bayon);
        nextRequest();

        String ids = province.getId().toString();
        mvc.perform(get("/api/v1/popular").param("provinceIds", ids))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DESTINATION"))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].id").value(bayon.getId().toString()))
                .andExpect(jsonPath("$.items[0].browses").value(1))
                .andExpect(jsonPath("$.items[0].reason").value("MOST_BROWSED"))
                .andExpect(jsonPath("$.items[1].id").value(tonle.getId().toString()))
                .andExpect(jsonPath("$.items[1].reason").value("TOP_RATED"))
                .andExpect(jsonPath("$.items[2].id").value(angkor.getId().toString()))
                .andExpect(jsonPath("$.items[2].reason").value("CATALOGUE"))
                .andExpect(jsonPath("$.items[2].browses").value(0))
                // The older shape still reads.
                .andExpect(jsonPath("$.destinations", hasSize(3)))
                .andExpect(jsonPath("$.destinations[0].destinationId").value(bayon.getId().toString()));
        mvc.perform(get("/api/v1/popular").param("provinceIds", ids).param("limit", "1"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].reason").value("MOST_BROWSED"));
        mvc.perform(get("/api/v1/popular").param("regionIds", province.getRegion().getId() + "," + elsewhere.getRegion().getId()))
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.regionId").isEmpty());
        mvc.perform(get("/api/v1/popular").param("regionId", province.getRegion().getId().toString()))
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.regionId").value(province.getRegion().getId().toString()));
    }

    @Test
    void otherKindsHaveListsOfTheirOwnAndTheirPagesCount() throws Exception {
        mvc.perform(get("/api/v1/popular").param("type", "STAY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("STAY"))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.destinations", hasSize(0)));
        mvc.perform(post("/api/v1/popular/events").with(customer("stayfan@visit.test")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"STAY\",\"targetId\":\"" + UUID.randomUUID() + "\",\"eventType\":\"STAY_VIEW\"}"))
                .andExpect(status().isAccepted());
        mvc.perform(post("/api/v1/popular/events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"BEACH\",\"targetId\":\"" + UUID.randomUUID() + "\",\"eventType\":\"VIEW\"}"))
                .andExpect(status().isBadRequest());
    }

    private void browse(Destination place) throws Exception {
        mvc.perform(post("/api/v1/popular/events").with(customer("popular@visit.test")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + place.getId() + "\",\"eventType\":\"DESTINATION_VIEW\"}"))
                .andExpect(status().isAccepted());
    }

    private void nextRequest() {
        entityManager.flush();
        entityManager.clear();
    }
}
