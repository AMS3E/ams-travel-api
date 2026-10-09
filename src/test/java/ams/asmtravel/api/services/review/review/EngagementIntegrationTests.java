package ams.asmtravel.api.services.review.review;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.food.domain.Food;
import ams.asmtravel.api.services.destination.food.domain.FoodType;
import ams.asmtravel.api.services.taxonomy.category.domain.Category;
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

import static ams.asmtravel.api.support.Callers.contributor;
import static ams.asmtravel.api.support.Callers.customer;
import static ams.asmtravel.api.support.Callers.editor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What visitors do with a place: review it (and so rate it), save it, open it
 * (and so make it popular, and shape what is recommended to them). And the
 * one thing the back office hears about: an answer to a publish request.
 */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class EngagementIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired TestCatalogue catalogue;

    private Province province;
    private Destination temple;

    @BeforeEach
    void place() {
        province = catalogue.province();
        temple = catalogue.published(province, "Banteay Srei", 13.5990, 103.9630);
    }

    @Test
    void starsSentWithAReviewBecomeTheAuthorsRating() throws Exception {
        String first = review(customer("first@visit.test"), 4);
        String hidden = review(customer("second@visit.test"), 5);
        mvc.perform(post("/api/v1/reviews").with(customer("first@visit.test")).contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(3)))
                .andExpect(status().isConflict());
        nextRequest();

        mvc.perform(get("/api/v1/ratings/summary").param("targetType", "DESTINATION").param("targetIds", temple.getId().toString()))
                .andExpect(jsonPath("$[0].average").value(4.5))
                .andExpect(jsonPath("$[0].count").value(2))
                .andExpect(jsonPath("$[0].stars['4']").value(1))
                .andExpect(jsonPath("$[0].stars['5']").value(1));

        // Its author edits what it says, not whether it shows; a moderator does the reverse.
        mvc.perform(put("/api/v1/reviews/" + hidden).with(customer("second@visit.test")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/reviews/" + hidden).with(customer("first@visit.test")).contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(1)))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/reviews/" + hidden).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\"}"))
                .andExpect(jsonPath("$.status").value("HIDDEN"))
                .andExpect(jsonPath("$.rating").value(5));
        mvc.perform(get("/api/v1/reviews")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/reviews").with(customer("second@visit.test"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reviews").with(editor())).andExpect(jsonPath("$.content[*].id", hasItem(hidden)));
        // Hiding a review hides its words; the stars are the person's rating and still count.
        nextRequest();
        mvc.perform(get("/api/v1/ratings/summary").param("targetType", "DESTINATION").param("targetIds", temple.getId().toString()))
                .andExpect(jsonPath("$[0].average").value(4.5))
                .andExpect(jsonPath("$[0].count").value(2));
        mvc.perform(get("/api/v1/reviews").param("targetType", "DESTINATION").param("targetId", temple.getId().toString()))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(first));
        mvc.perform(get("/api/v1/reviews/me").with(customer("second@visit.test")))
                .andExpect(jsonPath("$.content[0].status").value("HIDDEN"));

        mvc.perform(delete("/api/v1/reviews/" + first).with(customer("second@visit.test")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/reviews/" + first).with(customer("first@visit.test")))
                .andExpect(status().isNoContent());
        nextRequest();
        mvc.perform(get("/api/v1/ratings/summary").param("targetType", "DESTINATION").param("targetIds", temple.getId().toString()))
                .andExpect(jsonPath("$[0].average").value(4.5))
                .andExpect(jsonPath("$[0].count").value(2));
    }

    @Test
    void aVisitorSavesPlaces() throws Exception {
        RequestPostProcessor me = customer("saver@visit.test");
        String favorite = "{\"targetType\":\"DESTINATION\",\"targetId\":\"" + temple.getId() + "\"}";
        mvc.perform(post("/api/v1/favorites").with(me).contentType(MediaType.APPLICATION_JSON).content(favorite))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/favorites").with(me))
                .andExpect(jsonPath("$[0].targetId").value(temple.getId().toString()));
        mvc.perform(delete("/api/v1/favorites/DESTINATION/" + temple.getId()).with(me)).andExpect(status().isNoContent());
        nextRequest();
        mvc.perform(get("/api/v1/favorites").with(me)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/v1/favorites")).andExpect(status().isUnauthorized());
    }

    /** Popular counts different signed-in visitors; Recommended leans on what this visitor opened. */
    @Test
    void browsingMakesPopularAndRecommended() throws Exception {
        Category temples = catalogue.category("Temples");
        Category beaches = catalogue.category("Beaches");
        Destination bayon = catalogue.place(province, "Bayon", 13.4412, 103.8591, ListingStatus.PUBLISHED, null, Set.of(temples));
        Destination preahKo = catalogue.place(province, "Preah Ko", 13.3440, 103.9750, ListingStatus.PUBLISHED, null, Set.of(temples));
        Destination beach = catalogue.place(province, "Otres", 10.5700, 103.5400, ListingStatus.PUBLISHED, null, Set.of(beaches));

        for (String visitor : new String[] {"a@visit.test", "b@visit.test", "c@visit.test"}) {
            browse(customer(visitor), bayon, null);
        }
        browse(customer("a@visit.test"), bayon, null);
        browse(customer("a@visit.test"), beach, null);
        browse(null, bayon, "session-1");
        browse(null, bayon, "session-1");
        nextRequest();

        mvc.perform(get("/api/v1/popular"))
                .andExpect(jsonPath("$.destinations[0].destinationId").value(bayon.getId().toString()))
                .andExpect(jsonPath("$.destinations[0].browses").value(3))
                .andExpect(jsonPath("$.destinations[1].destinationId").value(beach.getId().toString()));
        // Each signed-in visitor once, and the anonymous session once: four views, not six.
        mvc.perform(get("/api/v1/destinations/" + bayon.getId())).andExpect(jsonPath("$.viewCount").value(4));

        mvc.perform(get("/api/v1/recommendations/for-you").with(customer("b@visit.test")))
                .andExpect(jsonPath("$.personal").value(true))
                .andExpect(jsonPath("$.destinations[0].destinationId").value(preahKo.getId().toString()))
                .andExpect(jsonPath("$.destinations[0].reason").value("LIKE_YOUR_BROWSING"))
                .andExpect(jsonPath("$.destinations[*].destinationId", not(hasItem(bayon.getId().toString()))));
        mvc.perform(get("/api/v1/recommendations/for-you"))
                .andExpect(jsonPath("$.personal").value(false))
                .andExpect(jsonPath("$.destinations[0].destinationId").value(bayon.getId().toString()))
                .andExpect(jsonPath("$.destinations[0].reason").value("MOST_BROWSED"));
    }

    /** A contributor hears when the desk answers their request. */
    @Test
    void anAnswerToAPublishRequestIsANotification() throws Exception {
        RequestPostProcessor writer = contributor("notified@desk.test");
        String draft = JsonPath.read(mvc.perform(post("/api/v1/destinations").with(writer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Beng Mealea\",\"provinceId\":\"" + province.getId() + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        String request = JsonPath.read(mvc.perform(post("/api/v1/publish-requests").with(writer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"DESTINATION\",\"targetId\":\"" + draft + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/api/v1/publish-requests/" + request + "/approve").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/publish-requests/" + request + "/publish").with(editor()))
                .andExpect(status().isOk());
        nextRequest();

        mvc.perform(get("/api/v1/notifications").with(writer))
                .andExpect(jsonPath("$[0].type").value("PUBLISH_APPROVED"))
                .andExpect(jsonPath("$[0].title").value("Beng Mealea is live"))
                .andExpect(jsonPath("$[0].data.destinationId").value(draft))
                .andExpect(jsonPath("$[0].isRead").value(false));
        mvc.perform(get("/api/v1/notifications").with(customer("other@visit.test"))).andExpect(jsonPath("$", hasSize(0)));
    }

    /* ---------------------------------------------------------------------- */


    @Test
    void aFoodListingKeepsItsRatingUpToDate() throws Exception {
        Food noodles = new Food();
        noodles.setName("Kuy Teav Corner");
        noodles.setSlug("kuy-teav-corner");
        noodles.setType(FoodType.RESTAURANT);
        noodles.setStatus(ListingStatus.PUBLISHED);
        entityManager.persist(noodles);
        entityManager.flush();

        for (var review : new Object[][] {{"a@visit.test", 4}, {"b@visit.test", 5}}) {
            mvc.perform(post("/api/v1/reviews").with(customer((String) review[0])).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetType\":\"FOOD\",\"targetId\":\"" + noodles.getId() + "\",\"rating\":" + review[1]
                                    + ",\"title\":\"Rich broth\",\"content\":\"Worth the queue.\"}"))
                    .andExpect(status().isCreated());
        }
        entityManager.clear();
        Food stored = entityManager.find(Food.class, noodles.getId());
        assertThat(stored.getAverageRating()).isEqualByComparingTo("4.5");
        assertThat(stored.getReviewCount()).isEqualTo(2);
    }
    private String review(RequestPostProcessor author, int rating) throws Exception {
        return JsonPath.read(mvc.perform(post("/api/v1/reviews").with(author).contentType(MediaType.APPLICATION_JSON)
                        .content(reviewJson(rating)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String reviewJson(int rating) {
        return "{\"targetType\":\"DESTINATION\",\"targetId\":\"" + temple.getId() + "\",\"rating\":" + rating
                + ",\"title\":\"Pink sandstone\",\"content\":\"Carvings as fine as lace.\"}";
    }

    private void browse(RequestPostProcessor visitor, Destination place, String session) throws Exception {
        var request = post("/api/v1/popular/events").contentType(MediaType.APPLICATION_JSON)
                .content("{\"destinationId\":\"" + place.getId() + "\",\"eventType\":\"DESTINATION_VIEW\""
                        + (session == null ? "" : ",\"sessionId\":\"" + session + "\"") + "}");
        mvc.perform(visitor == null ? request : request.with(visitor)).andExpect(status().isAccepted());
    }

    private void nextRequest() {
        entityManager.flush();
        entityManager.clear();
    }
}
