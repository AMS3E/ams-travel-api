package ams.asmtravel.api.services.search;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.food.domain.Food;
import ams.asmtravel.api.services.destination.food.domain.FoodType;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.region.domain.Region;
import ams.asmtravel.api.services.location.region.domain.RegionKind;
import ams.asmtravel.api.services.review.rating.domain.Rating;
import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;
import ams.asmtravel.api.support.TestCatalogue;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The filters the app's Search sheet and Provinces page asked for on 2026-10-14: several regions or
 * provinces at once, only the Traveler seal holders, the whole catalogue grouped by province, and the
 * open-now line on a food card. The seal's bar is lowered to one rating so a test can reach it.
 */
@SpringBootTest(properties = {"app.security.oauth2.enabled=true", "app.verification.traveler-seal.min-ratings=1"})
@AutoConfigureMockMvc
@Transactional
class SearchFiltersIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired TestCatalogue catalogue;

    @Test
    void severalProvincesOrRegionsMatchAtOnce() throws Exception {
        Province siemReap = catalogue.province();
        Province battambang = catalogue.province();
        Province kampot = catalogue.province();
        Destination angkor = catalogue.published(siemReap, "Angkor Wat", 13.4125, 103.8670);
        catalogue.published(battambang, "Bamboo Train", 13.0800, 103.2100);
        catalogue.published(kampot, "Bokor Hill", 10.6300, 104.0300);
        Region ancient = new Region();
        ancient.setName("Ancient Capitals F");
        ancient.setCode("ANCIENT-F");
        ancient.setKind(RegionKind.TOURISM);
        entityManager.persist(ancient);
        angkor.getTourismRegions().add(ancient);
        nextRequest();

        mvc.perform(get("/api/v1/search").param("provinceIds", siemReap.getId() + "," + battambang.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Angkor Wat", "Bamboo Train")));
        // The single parameter and the list add up.
        mvc.perform(get("/api/v1/search").param("provinceId", kampot.getId().toString()).param("provinceIds", siemReap.getId().toString()))
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Angkor Wat", "Bokor Hill")));
        // A zone reaches its provinces' places; a tourism region its shelf; both in one list.
        mvc.perform(get("/api/v1/search").param("regionIds", battambang.getRegion().getId() + "," + kampot.getRegion().getId()))
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Bamboo Train", "Bokor Hill")));
        mvc.perform(get("/api/v1/search").param("regionIds", ancient.getId() + "," + kampot.getRegion().getId()))
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Angkor Wat", "Bokor Hill")));
        mvc.perform(get("/api/v1/search").param("regionIds", UUID.randomUUID().toString()))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void onlyTheTravelerSealHoldersWhenAsked() throws Exception {
        Province province = catalogue.province();
        Destination angkor = catalogue.published(province, "Angkor Wat S", 13.4125, 103.8670);
        catalogue.published(province, "Bayon S", 13.4413, 103.8590);
        entityManager.persist(new Rating(UUID.randomUUID(), RatingTargetType.DESTINATION, angkor.getId(), 5));
        nextRequest();

        mvc.perform(get("/api/v1/search").param("provinceId", province.getId().toString()))
                .andExpect(jsonPath("$.content", hasSize(2)));
        mvc.perform(get("/api/v1/search").param("provinceId", province.getId().toString()).param("travelerRecommended", "true"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Angkor Wat S"))
                .andExpect(jsonPath("$.content[0].travelerRecommended").value(true))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        // Only destinations carry the seal.
        mvc.perform(get("/api/v1/search").param("type", "STAY").param("travelerRecommended", "true"))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void theProvincesPageComesGroupedInOneAnswer() throws Exception {
        Province first = catalogue.province();
        Province second = catalogue.province();
        catalogue.published(first, "Angkor Wat G", 13.4125, 103.8670);
        catalogue.published(first, "Bayon G", 13.4413, 103.8590);
        catalogue.published(first, "Ta Prohm G", 13.4350, 103.8890);
        catalogue.published(second, "Bamboo Train G", 13.0800, 103.2100);
        nextRequest();

        mvc.perform(get("/api/v1/search/by-province").param("perProvince", "2")
                        .param("provinceIds", first.getId() + "," + second.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].provinceName").value(first.getName()))
                .andExpect(jsonPath("$[0].totalElements").value(3))
                .andExpect(jsonPath("$[0].content", hasSize(2)))
                .andExpect(jsonPath("$[0].content[0].name").value("Angkor Wat G"))
                .andExpect(jsonPath("$[1].provinceId").value(second.getId().toString()))
                .andExpect(jsonPath("$[1].totalElements").value(1));
        // A province with nothing matching is not listed.
        mvc.perform(get("/api/v1/search/by-province").param("q", "bamboo"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].provinceId").value(second.getId().toString()));
        mvc.perform(get("/api/v1/search/by-province").param("perProvince", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void aFoodCardSaysWhetherItIsOpen() throws Exception {
        Food allDay = new Food();
        allDay.setName("Always Open Noodles");
        allDay.setSlug("always-open-noodles");
        allDay.setType(FoodType.RESTAURANT);
        allDay.setStatus(ListingStatus.PUBLISHED);
        allDay.setOpeningHours(Map.of("mon", "00:00-24:00", "tue", "00:00-24:00", "wed", "00:00-24:00", "thu", "00:00-24:00",
                "fri", "00:00-24:00", "sat", "00:00-24:00", "sun", "00:00-24:00"));
        entityManager.persist(allDay);
        Food unknown = new Food();
        unknown.setName("No Hours Yet");
        unknown.setSlug("no-hours-yet");
        unknown.setType(FoodType.CAFE);
        unknown.setStatus(ListingStatus.PUBLISHED);
        entityManager.persist(unknown);
        nextRequest();

        mvc.perform(get("/api/v1/search").param("type", "FOOD").param("q", "always"))
                .andExpect(jsonPath("$.content[0].openNow").value(true))
                .andExpect(jsonPath("$.content[0].todayHours").value("00:00-24:00"));
        mvc.perform(get("/api/v1/search").param("type", "FOOD").param("q", "no hours"))
                .andExpect(jsonPath("$.content[0].openNow").isEmpty())
                .andExpect(jsonPath("$.content[0].todayHours").isEmpty());
        mvc.perform(get("/api/v1/foods/" + allDay.getId()))
                .andExpect(jsonPath("$.openNow").value(true))
                .andExpect(jsonPath("$.todayHours").value("00:00-24:00"));
    }

    private void nextRequest() {
        entityManager.flush();
        entityManager.clear();
    }
}
