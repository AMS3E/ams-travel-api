package ams.asmtravel.api.services.review.review;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
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

import static ams.asmtravel.api.support.Callers.customer;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The "5 ★ … 1 ★" menu on a place's reviews and on a person's own: the star filter runs on the server. */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class ReviewStarFilterIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired TestCatalogue catalogue;

    @Test
    void reviewsAreNarrowedByTheirAuthorsStars() throws Exception {
        Province province = catalogue.province();
        Destination temple = catalogue.published(province, "Preah Vihear", 14.3900, 104.6800);
        Destination other = catalogue.published(province, "Koh Ker", 13.7800, 104.5300);
        RequestPostProcessor delighted = customer("five@visit.test");
        RequestPostProcessor lukewarm = customer("three@visit.test");
        review(delighted, temple, 5);
        review(lukewarm, temple, 3);
        review(lukewarm, other, 5);
        entityManager.flush();
        entityManager.clear();

        String place = temple.getId().toString();
        mvc.perform(get("/api/v1/reviews").param("targetType", "DESTINATION").param("targetId", place))
                .andExpect(jsonPath("$.content", hasSize(2)));
        mvc.perform(get("/api/v1/reviews").param("targetType", "DESTINATION").param("targetId", place).param("rating", "5"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].rating").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mvc.perform(get("/api/v1/reviews").param("targetType", "DESTINATION").param("targetId", place).param("rating", "3,5"))
                .andExpect(jsonPath("$.content", hasSize(2)));
        mvc.perform(get("/api/v1/reviews").param("targetType", "DESTINATION").param("targetId", place).param("rating", "1"))
                .andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/api/v1/reviews").param("targetType", "DESTINATION").param("targetId", place).param("rating", "7"))
                .andExpect(status().isBadRequest());

        // Mine: across every place I reviewed.
        mvc.perform(get("/api/v1/reviews/me").with(lukewarm)).andExpect(jsonPath("$.content", hasSize(2)));
        mvc.perform(get("/api/v1/reviews/me").with(lukewarm).param("rating", "5"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].targetId").value(other.getId().toString()));
        mvc.perform(get("/api/v1/reviews/me").with(lukewarm).param("rating", "4"))
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    private void review(RequestPostProcessor author, Destination place, int rating) throws Exception {
        mvc.perform(post("/api/v1/reviews").with(author).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"DESTINATION\",\"targetId\":\"" + place.getId() + "\",\"rating\":" + rating
                                + ",\"title\":\"Stone\",\"content\":\"Worth the road.\"}"))
                .andExpect(status().isCreated());
    }
}
