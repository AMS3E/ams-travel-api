package ams.asmtravel.api.services.search;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.region.domain.Region;
import ams.asmtravel.api.services.location.region.domain.RegionKind;
import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.support.TestCatalogue;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The app searches and filters on the server, and draws its list rows from one kind of card. */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class SearchIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired TestCatalogue catalogue;

    @Test
    void theAppSearchesAndFiltersPublishedPlacesOnTheServer() throws Exception {
        Province province = catalogue.province();
        Category temples = catalogue.category("Temples");
        Category beaches = catalogue.category("Beaches");
        Destination angkor = catalogue.place(province, "Angkor Wat", 13.4125, 103.8670, ListingStatus.PUBLISHED, null, Set.of(temples));
        catalogue.place(province, "Bayon Temple", 13.4413, 103.8590, ListingStatus.PUBLISHED, null, Set.of(temples));
        Destination otres = catalogue.place(province, "Otres Beach", 10.5700, 103.5400, ListingStatus.PUBLISHED, null, Set.of(beaches));
        catalogue.place(province, "Angkor Draft", 13.4000, 103.8000, ListingStatus.DRAFT, null, Set.of(temples));
        Region ancient = new Region();
        ancient.setName("Ancient Capitals");
        ancient.setCode("ANCIENT-S");
        ancient.setKind(RegionKind.TOURISM);
        entityManager.persist(ancient);
        angkor.getTourismRegions().add(ancient);
        entityManager.flush();
        entityManager.clear();

        // Words: the draft is never found; a card carries its category and province names.
        mvc.perform(get("/api/v1/search").param("q", "angkor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("DESTINATION"))
                .andExpect(jsonPath("$.content[0].name").value("Angkor Wat"))
                .andExpect(jsonPath("$.content[0].categoryNames[0]").value("Temples"))
                .andExpect(jsonPath("$.content[0].provinceName").value(province.getName()))
                .andExpect(jsonPath("$.content[0].ratingCount").value(0))
                .andExpect(jsonPath("$.content[0].verified").value(false));
        // A category chip, a region chip, a province chip; name order and paging.
        mvc.perform(get("/api/v1/search").param("categoryIds", temples.getId().toString()))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].name").value("Angkor Wat"))
                .andExpect(jsonPath("$.content[1].name").value("Bayon Temple"));
        mvc.perform(get("/api/v1/search").param("regionId", ancient.getId().toString()))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].tourismRegionIds[0]").value(ancient.getId().toString()));
        mvc.perform(get("/api/v1/search").param("provinceId", province.getId().toString()))
                .andExpect(jsonPath("$.content", hasSize(3)));
        mvc.perform(get("/api/v1/search").param("provinceId", UUID.randomUUID().toString()))
                .andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(get("/api/v1/search").param("size", "1").param("page", "1"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Bayon Temple"));
        // Other kinds search the same way; nothing is listed yet.
        mvc.perform(get("/api/v1/search").param("type", "STAY").param("q", "villa"))
                .andExpect(jsonPath("$.content", hasSize(0)));
        // Suggestions while typing: names that start with the text first.
        mvc.perform(get("/api/v1/search/suggest").param("q", "ang"))
                .andExpect(jsonPath("$[?(@.kind == 'DESTINATION' && @.name == 'Angkor Wat')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.name == 'Angkor Draft')]", hasSize(0)));
        mvc.perform(get("/api/v1/search/suggest").param("q", " "))
                .andExpect(status().isBadRequest());
        // Cards for known ids, in the order asked: what Saves, Popular and Nearby draw their rows from.
        mvc.perform(get("/api/v1/search/cards").param("ids", otres.getId() + "," + angkor.getId()))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Otres Beach"))
                .andExpect(jsonPath("$[1].name").value("Angkor Wat"));
        // Popular takes the same province and interest chips.
        mvc.perform(get("/api/v1/popular").param("provinceId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destinations", hasSize(0)));
    }
}
