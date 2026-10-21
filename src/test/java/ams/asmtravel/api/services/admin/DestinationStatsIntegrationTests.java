package ams.asmtravel.api.services.admin;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.food.domain.Food;
import ams.asmtravel.api.services.destination.food.domain.FoodType;
import ams.asmtravel.api.services.destination.food.repository.FoodRepository;
import ams.asmtravel.api.services.destination.stay.domain.Stay;
import ams.asmtravel.api.services.destination.stay.domain.StayType;
import ams.asmtravel.api.services.destination.stay.repository.StayRepository;
import ams.asmtravel.api.support.TestCatalogue;
import io.minio.MinioClient;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static ams.asmtravel.api.support.Callers.contributor;
import static ams.asmtravel.api.support.Callers.customer;
import static ams.asmtravel.api.support.Callers.editor;
import static ams.asmtravel.api.support.Callers.owner;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The Destination Stats page reads every place's standing in one call; an editorial desk's read. */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class DestinationStatsIntegrationTests {

    @MockitoBean MinioClient minioClient;
    @Autowired MockMvc mvc;
    @Autowired TestCatalogue catalogue;
    @Autowired EntityManager entityManager;
    @Autowired StayRepository stayRepository;
    @Autowired FoodRepository foodRepository;

    @Test
    void everyPlacesRatingsBrowsesAndStandingInOneRead() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        Destination bakheng = catalogue.published(catalogue.province(), "Phnom Bakheng", 13.4240, 103.8560);
        String id = bakheng.getId().toString();
        String row = "$.places[?(@.id=='" + id + "')]";

        mvc.perform(get("/api/v1/admin/destination-stats").with(editor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(row + ".name").value("Phnom Bakheng"))
                .andExpect(jsonPath(row + ".ratingCount").value(0))
                .andExpect(jsonPath(row + ".browses").value(0))
                .andExpect(jsonPath(row + ".verified").value(false));

        // A review with its stars, a browse, and an official site's verification all show up.
        mvc.perform(post("/api/v1/reviews").with(customer("a@visit.test")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"DESTINATION\",\"targetId\":\"" + id + "\",\"rating\":4,\"content\":\"Sunset crowds\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/popular/events").with(customer("b@visit.test")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + id + "\",\"eventType\":\"DESTINATION_VIEW\"}"))
                .andExpect(status().isAccepted());
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + id + "\",\"verified\":true,\"basis\":\"OFFICIAL_SITE\"}"))
                .andExpect(status().isCreated());
        // The view count is bumped by a bulk update; read the place afresh, as a later request would.
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/api/v1/admin/destination-stats").with(editor()))
                .andExpect(jsonPath(row + ".ratingCount").value(1))
                .andExpect(jsonPath(row + ".averageRating").value(4.0))
                .andExpect(jsonPath(row + ".reviewCount").value(1))
                .andExpect(jsonPath(row + ".browses").value(1))
                .andExpect(jsonPath(row + ".viewCount").value(1))
                .andExpect(jsonPath(row + ".verified").value(true))
                .andExpect(jsonPath(row + ".basis").value("OFFICIAL_SITE"))
                .andExpect(jsonPath(row + ".travelerRecommended").value(false));

        // A period narrows what travellers did; views, the badge and the status stay as of today.
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Phnom_Penh"));
        mvc.perform(get("/api/v1/admin/destination-stats").with(editor())
                        .param("from", today.toString()).param("to", today.toString()))
                .andExpect(jsonPath("$.from").value(today.toString()))
                .andExpect(jsonPath(row + ".ratingCount").value(1))
                .andExpect(jsonPath(row + ".reviewCount").value(1))
                .andExpect(jsonPath(row + ".browses").value(1));
        mvc.perform(get("/api/v1/admin/destination-stats").with(editor())
                        .param("from", today.plusDays(1).toString()).param("to", today.plusDays(1).toString()))
                .andExpect(jsonPath(row + ".ratingCount").value(0))
                .andExpect(jsonPath(row + ".averageRating", hasItem(nullValue())))
                .andExpect(jsonPath(row + ".reviewCount").value(0))
                .andExpect(jsonPath(row + ".browses").value(0))
                .andExpect(jsonPath(row + ".viewCount").value(1))
                .andExpect(jsonPath(row + ".verified").value(true));
        mvc.perform(get("/api/v1/admin/destination-stats").with(editor())
                        .param("from", today.plusDays(1).toString()).param("to", today.toString()))
                .andExpect(status().isBadRequest());

        // A contributor edits their own drafts; the catalogue's standing is the editorial desks' to read.
        mvc.perform(get("/api/v1/admin/destination-stats").with(contributor("writer@stats.test"))).andExpect(status().isForbidden());
    }

    /** Destination Detail reads one place by its id whatever its kind; a stay's or a food place's reviews, browses and views count the same way. */
    @Test
    void onePlacesStandingByIdWhateverItsKind() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        Destination bakheng = catalogue.published(catalogue.province(), "Phnom Bakheng", 13.4240, 103.8560);
        Stay stay = new Stay();
        stay.setName("Memo Guesthouse");
        stay.setType(StayType.GUESTHOUSE);
        stay.setStatus(ListingStatus.PUBLISHED);
        stay.setCreatedAt(Instant.now());
        stay.setUpdatedAt(Instant.now());
        String stayId = stayRepository.save(stay).getId().toString();
        Food food = new Food();
        food.setName("Memo Noodles");
        food.setSlug("memo-noodles");
        food.setType(FoodType.RESTAURANT);
        food.setStatus(ListingStatus.PUBLISHED);
        food.setCreatedAt(Instant.now());
        food.setUpdatedAt(Instant.now());
        String foodId = foodRepository.save(food).getId().toString();

        mvc.perform(get("/api/v1/admin/destination-stats/" + bakheng.getId()).with(editor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.place.type").value("DESTINATION"))
                .andExpect(jsonPath("$.place.name").value("Phnom Bakheng"))
                .andExpect(jsonPath("$.place.verified").value(false));

        // A review of the stay, and a visitor opening its page, count on it; the open is its first view.
        mvc.perform(post("/api/v1/reviews").with(customer("a@visit.test")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"STAY\",\"targetId\":\"" + stayId + "\",\"rating\":5,\"content\":\"Quiet rooms\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/popular/events").with(customer("b@visit.test")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"STAY\",\"targetId\":\"" + stayId + "\",\"eventType\":\"STAY_VIEW\"}"))
                .andExpect(status().isAccepted());
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/api/v1/admin/destination-stats/" + stayId).with(editor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.place.type").value("STAY"))
                .andExpect(jsonPath("$.place.name").value("Memo Guesthouse"))
                .andExpect(jsonPath("$.place.slug").value(nullValue()))
                .andExpect(jsonPath("$.place.ratingCount").value(1))
                .andExpect(jsonPath("$.place.averageRating").value(5.0))
                .andExpect(jsonPath("$.place.reviewCount").value(1))
                .andExpect(jsonPath("$.place.browses").value(1))
                .andExpect(jsonPath("$.place.viewCount").value(1))
                .andExpect(jsonPath("$.place.verified").value(false))
                .andExpect(jsonPath("$.place.basis").value(nullValue()));
        mvc.perform(get("/api/v1/stays/" + stayId).with(editor())).andExpect(jsonPath("$.viewCount").value(1));

        mvc.perform(get("/api/v1/admin/destination-stats/" + foodId).with(editor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.place.type").value("FOOD"))
                .andExpect(jsonPath("$.place.slug").value("memo-noodles"))
                .andExpect(jsonPath("$.place.browses").value(0))
                .andExpect(jsonPath("$.place.viewCount").value(0));

        // A period narrows what travellers did on a stay as on an attraction; the views stay as of today.
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Phnom_Penh"));
        mvc.perform(get("/api/v1/admin/destination-stats/" + stayId).with(editor())
                        .param("from", today.plusDays(1).toString()).param("to", today.plusDays(1).toString()))
                .andExpect(jsonPath("$.place.ratingCount").value(0))
                .andExpect(jsonPath("$.place.browses").value(0))
                .andExpect(jsonPath("$.place.viewCount").value(1));

        mvc.perform(get("/api/v1/admin/destination-stats/" + UUID.randomUUID()).with(editor())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/admin/destination-stats/" + stayId).with(contributor("writer@stats.test"))).andExpect(status().isForbidden());
    }
}
