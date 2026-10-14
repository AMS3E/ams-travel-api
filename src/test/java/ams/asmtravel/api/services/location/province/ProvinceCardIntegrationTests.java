package ams.asmtravel.api.services.location.province;

import ams.asmtravel.api.services.location.province.domain.Province;
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

import static ams.asmtravel.api.support.Callers.owner;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A province card in the app: its picture, its centre for the Map screen, and what its places add up
 * to. A region answers its centre and the box around its places the same way.
 */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class ProvinceCardIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired TestCatalogue catalogue;

    @Test
    void aProvinceCarriesItsPlacesCoverAndCentre() throws Exception {
        Province province = catalogue.province();
        catalogue.published(province, "Angkor Wat C", 13.4125, 103.8670);
        catalogue.published(province, "Bayon C", 13.4413, 103.8590);
        entityManager.flush();
        entityManager.clear();
        String id = province.getId().toString();

        mvc.perform(get("/api/v1/provinces/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeCount").value(2))
                .andExpect(jsonPath("$.reviewCount").value(0))
                .andExpect(jsonPath("$.averageRating").isEmpty())
                .andExpect(jsonPath("$.coverUrl").isEmpty())
                .andExpect(jsonPath("$.latitude").isEmpty())
                .andExpect(jsonPath("$.bounds.north").value(13.4413))
                .andExpect(jsonPath("$.bounds.south").value(13.4125))
                .andExpect(jsonPath("$.bounds.east").value(103.867))
                .andExpect(jsonPath("$.bounds.west").value(103.859));

        String cover = "province/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".jpg";
        mvc.perform(put("/api/v1/provinces/" + id).with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + province.getName() + "\",\"coverKey\":\"" + cover + "\",\"latitude\":13.3671,\"longitude\":103.8448}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverKey").value(cover))
                .andExpect(jsonPath("$.coverUrl").value("http://localhost:8080/api/v1/media/files/" + cover))
                .andExpect(jsonPath("$.latitude").value(13.3671))
                .andExpect(jsonPath("$.longitude").value(103.8448));
        // Half a centre is a mistake; no centre keeps the one there is.
        mvc.perform(put("/api/v1/provinces/" + id).with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + province.getName() + "\",\"latitude\":13.0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/provinces/" + id).with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + province.getName() + "\"}"))
                .andExpect(jsonPath("$.latitude").value(13.3671))
                .andExpect(jsonPath("$.coverKey").value(cover));
        mvc.perform(get("/api/v1/provinces"))
                .andExpect(jsonPath("$[?(@.id == '" + id + "')].placeCount").value(org.hamcrest.Matchers.contains(2)));
    }

    @Test
    void aRegionAnswersTheMiddleOfItsPlacesUntilACentreIsSet() throws Exception {
        Province province = catalogue.province();
        catalogue.published(province, "Kep Beach R", 10.4800, 104.3000);
        catalogue.published(province, "Bokor R", 10.6200, 104.0200);
        entityManager.flush();
        entityManager.clear();
        String zone = province.getRegion().getId().toString();

        mvc.perform(get("/api/v1/regions/" + zone))
                .andExpect(jsonPath("$.latitude").value(10.55))
                .andExpect(jsonPath("$.longitude").value(104.16))
                .andExpect(jsonPath("$.centreSet").value(false))
                .andExpect(jsonPath("$.bounds.north").value(10.62))
                .andExpect(jsonPath("$.bounds.west").value(104.02));
        mvc.perform(put("/api/v1/regions/" + zone).with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Coast R\",\"latitude\":10.5,\"longitude\":104.1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(10.5))
                .andExpect(jsonPath("$.centreSet").value(true))
                .andExpect(jsonPath("$.bounds.north").value(10.62));
        mvc.perform(get("/api/v1/regions/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void aCorridorHasAGallery() throws Exception {
        mvc.perform(get("/api/v1/media").param("ownerType", "CORRIDOR"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/media").param("ownerType", "REGION"))
                .andExpect(status().isBadRequest());
    }
}
