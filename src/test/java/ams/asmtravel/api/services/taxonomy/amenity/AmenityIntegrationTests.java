package ams.asmtravel.api.services.taxonomy.amenity;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class AmenityIntegrationTests {

    private static final String PIN = "\"latitude\":11.5564,\"longitude\":104.9282";

    @Autowired MockMvc mvc;

    /** The changelog ships the two fixed lists merged: 34 offered at the property, 47 in the room, nine at both. */
    @Test
    void theCatalogueShipsWithTheOldListsMerged() throws Exception {
        mvc.perform(get("/api/v1/amenities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(72)));
        mvc.perform(get("/api/v1/amenities").param("level", "PROPERTY"))
                .andExpect(jsonPath("$", hasSize(34)))
                .andExpect(jsonPath("$[?(@.slug == 'wifi')].level", contains("BOTH")))
                .andExpect(jsonPath("$[?(@.slug == 'hairdryer')]", hasSize(0)));
        mvc.perform(get("/api/v1/amenities").param("level", "ROOM"))
                .andExpect(jsonPath("$", hasSize(47)))
                .andExpect(jsonPath("$[?(@.slug == 'pool')]", hasSize(0)))
                .andExpect(jsonPath("$[?(@.slug == 'wifi')].iconKey", contains("wifi")));
    }

    /**
     * A curator adds an amenity; a stay lists it at its level and a room is
     * refused it; it cannot be deleted while listed, only retired, and a
     * retired one is still understood by the stay that carries it.
     */
    @Test
    void anAmenityIsManagedAsATaxonomy() throws Exception {
        RequestPostProcessor curator = staff("curator@desk.test", "ADMIN", "PERM_TAXONOMY_MANAGE",
                "PERM_DESTINATION_EDIT_ANY", "PERM_FULL_CONTROL");
        String cinema = id(mvc.perform(post("/api/v1/amenities").with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rooftop cinema\",\"groupName\":\"Wellbeing\",\"level\":\"PROPERTY\",\"iconKey\":\"monitor-play\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("rooftop-cinema"))
                .andExpect(jsonPath("$.isActive").value(true))
                /* Last in its group: Wellbeing ships with pool, spa, gym and garden. */
                .andExpect(jsonPath("$.sortOrder").value(4)));

        String stay = id(mvc.perform(post("/api/v1/stays").with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sky Hotel\",\"type\":\"HOTEL\",\"priceFrom\":80,"
                                + "\"amenities\":[\"wifi\",\"rooftop-cinema\"]," + PIN + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amenities", contains("wifi", "rooftop-cinema"))));

        /* A room is offered the room list: the property's cinema is refused, Wi-Fi (both) and a hairdryer are not. */
        mvc.perform(post("/api/v1/stay-rooms").with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stayId\":\"" + stay + "\",\"name\":\"Twin\",\"roomType\":\"TWIN\",\"singleBeds\":2,"
                                + "\"pricePerNight\":40,\"amenities\":[\"wifi\",\"rooftop-cinema\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No such room amenity: rooftop-cinema"));
        mvc.perform(post("/api/v1/stay-rooms").with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stayId\":\"" + stay + "\",\"name\":\"Twin\",\"roomType\":\"TWIN\",\"singleBeds\":2,"
                                + "\"pricePerNight\":40,\"amenities\":[\"wifi\",\"hairdryer\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amenities", contains("wifi", "hairdryer")));

        /* Listed by a stay: deleting is refused, retiring is the way. */
        mvc.perform(delete("/api/v1/amenities/" + cinema).with(curator))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("retire the amenity")));
        mvc.perform(put("/api/v1/amenities/" + cinema).with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rooftop cinema\",\"groupName\":\"Wellbeing\",\"level\":\"PROPERTY\",\"isActive\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        /* The stay that carries a retired amenity still saves with it; an unknown slug is still refused. */
        mvc.perform(put("/api/v1/stays/" + stay).with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sky Hotel\",\"type\":\"HOTEL\",\"priceFrom\":80,"
                                + "\"amenities\":[\"wifi\",\"rooftop-cinema\"]," + PIN + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amenities", hasSize(2)));
        mvc.perform(put("/api/v1/stays/" + stay).with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sky Hotel\",\"type\":\"HOTEL\",\"priceFrom\":80,"
                                + "\"amenities\":[\"jacuzzi\"]," + PIN + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No such amenity: jacuzzi"));

        /* Once nothing lists it, it can go. */
        mvc.perform(put("/api/v1/stays/" + stay).with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sky Hotel\",\"type\":\"HOTEL\",\"priceFrom\":80,\"amenities\":[\"wifi\"]," + PIN + "}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/amenities/" + cinema).with(curator))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/amenities/" + cinema))
                .andExpect(status().isNotFound());
    }

    /** Only a taxonomy manager writes; everyone reads. */
    @Test
    void readingIsPublicAndWritingIsTheTaxonomyManagers() throws Exception {
        mvc.perform(get("/api/v1/amenities")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/amenities").with(staff("partner@hotel.test", "PARTNER", "PERM_LISTING_EDIT_OWN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Helipad\",\"groupName\":\"Parking & transport\",\"level\":\"PROPERTY\"}"))
                .andExpect(status().isForbidden());
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private static RequestPostProcessor staff(String email, String... grants) {
        return jwt().jwt(token -> token.subject("subject-" + email).claim("email", email))
                .authorities(Arrays.stream(grants)
                        .map(grant -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + grant))
                        .toList());
    }
}
