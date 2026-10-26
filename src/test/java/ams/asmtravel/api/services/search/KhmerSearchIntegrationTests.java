package ams.asmtravel.api.services.search;

import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.corridor.domain.CorridorStatus;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.support.TestCatalogue;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Asked in Khmer, a card speaks Khmer where it is written and names what is still English; never English passed off as Khmer. */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class KhmerSearchIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired TestCatalogue catalogue;

    @Test
    void aKhmerCardUsesTheKhmerWrittenAndNamesWhatIsStillEnglish() throws Exception {
        Province province = catalogue.province();
        province.setNameKm("សៀមរាប");
        Category temples = catalogue.category("Temples");
        temples.setNameKm("ប្រាសាទ");
        Category hills = catalogue.category("Hills");
        Destination angkor = catalogue.place(province, "Angkor Wat", 13.4125, 103.8670, ListingStatus.PUBLISHED, null,
                Set.of(temples));
        angkor.setNameKm("អង្គរវត្ត");
        angkor.setDescriptionKm("<p>ប្រាសាទ <b>ធំបំផុត</b></p>");
        Destination bakheng = catalogue.place(province, "Phnom Bakheng", 13.4240, 103.8560, ListingStatus.PUBLISHED, null,
                Set.of(hills));
        bakheng.setShortDescription("A hill temple at sunset.");
        entityManager.flush();
        entityManager.clear();

        // English is untouched and says nothing is untranslated.
        mvc.perform(get("/api/v1/search").param("q", "angkor"))
                .andExpect(jsonPath("$.content[0].name").value("Angkor Wat"))
                .andExpect(jsonPath("$.content[0].provinceName").value(province.getName()))
                .andExpect(jsonPath("$.content[0].untranslated", hasSize(0)));

        // Khmer words find the place; asked in Khmer, the card is Khmer throughout.
        mvc.perform(get("/api/v1/search").param("q", "អង្គរ").param("lang", "km"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("អង្គរវត្ត"))
                .andExpect(jsonPath("$.content[0].shortDescription").value("ប្រាសាទ ធំបំផុត"))
                .andExpect(jsonPath("$.content[0].provinceName").value("សៀមរាប"))
                .andExpect(jsonPath("$.content[0].categoryNames", contains("ប្រាសាទ")))
                .andExpect(jsonPath("$.content[0].untranslated", hasSize(0)));

        // No Khmer written: the English shows, and the card says so field by field.
        mvc.perform(get("/api/v1/search/cards").param("ids", bakheng.getId().toString()).param("lang", "km"))
                .andExpect(jsonPath("$[0].name").value("Phnom Bakheng"))
                .andExpect(jsonPath("$[0].provinceName").value("សៀមរាប"))
                .andExpect(jsonPath("$[0].categoryNames", contains("Hills")))
                .andExpect(jsonPath("$[0].untranslated", contains("name", "shortDescription", "categoryNames")));

        mvc.perform(get("/api/v1/search/by-province").param("lang", "km"))
                .andExpect(jsonPath("$[0].provinceName").value("សៀមរាប"))
                .andExpect(jsonPath("$[0].untranslated", hasSize(0)));
        mvc.perform(get("/api/v1/search/suggest").param("q", "អង្គ").param("lang", "km"))
                .andExpect(jsonPath("$[?(@.kind == 'DESTINATION' && @.name == 'អង្គរវត្ត')]", hasSize(1)));
        mvc.perform(get("/api/v1/search/suggest").param("q", "bakh").param("lang", "km"))
                .andExpect(jsonPath("$[0].name").value("Phnom Bakheng"))
                .andExpect(jsonPath("$[0].untranslated", contains("name")));
        mvc.perform(get("/api/v1/search").param("lang", "fr")).andExpect(status().isBadRequest());
    }

    /** A corridor is found by its own Khmer and, asked in Khmer, shows it; one without names what is still English. */
    @Test
    void aCorridorCardUsesItsOwnKhmer() throws Exception {
        corridor("Lotus River Loop", "lotus-river-loop", "Boats between temples.", "ផ្លូវទន្លេផ្កាឈូក", "ទូករវាងប្រាសាទ");
        corridor("Pepper Coast Run", "pepper-coast-run", "Crab and pepper.", null, null);
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/api/v1/search").param("type", "CORRIDOR").param("q", "ផ្កាឈូក").param("lang", "km"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("ផ្លូវទន្លេផ្កាឈូក"))
                .andExpect(jsonPath("$.content[0].shortDescription").value("ទូករវាងប្រាសាទ"))
                .andExpect(jsonPath("$.content[0].untranslated", hasSize(0)));
        mvc.perform(get("/api/v1/search").param("type", "CORRIDOR").param("q", "pepper").param("lang", "km"))
                .andExpect(jsonPath("$.content[0].name").value("Pepper Coast Run"))
                .andExpect(jsonPath("$.content[0].untranslated", contains("name", "shortDescription")));
        mvc.perform(get("/api/v1/search").param("type", "CORRIDOR").param("q", "lotus"))
                .andExpect(jsonPath("$.content[0].name").value("Lotus River Loop"));
        mvc.perform(get("/api/v1/search/suggest").param("q", "ផ្កាឈូក").param("lang", "km"))
                .andExpect(jsonPath("$[?(@.kind == 'CORRIDOR' && @.name == 'ផ្លូវទន្លេផ្កាឈូក')]", hasSize(1)));
    }

    private void corridor(String name, String slug, String summary, String nameKm, String summaryKm) {
        Corridor corridor = new Corridor();
        corridor.setName(name);
        corridor.setSlug(slug);
        corridor.setShortDescription(summary);
        corridor.setNameKm(nameKm);
        corridor.setShortDescriptionKm(summaryKm);
        corridor.setStatus(CorridorStatus.PUBLISHED);
        entityManager.persist(corridor);
    }
}
