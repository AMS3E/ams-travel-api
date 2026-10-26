package ams.asmtravel.api.services.taxonomy.interestgroup;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class InterestGroupIntegrationTests {

    private static final String WATER = "general/00000000-0000-0000-0000-000000000000/interest-group-water.png";

    @Autowired MockMvc mvc;

    /** The changelog ships the six groups in the Interests tab's order, each with its seed picture. */
    @Test
    void theSixGroupsShipWithTheirPictures() throws Exception {
        mvc.perform(get("/api/v1/interest-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", contains("Attraction Sites", "Stays", "Food", "Water",
                        "Activities & Experiences", "Tourism Corridors")))
                .andExpect(jsonPath("$[3].coverKey").value(WATER))
                .andExpect(jsonPath("$[3].coverUrl", endsWith("/" + WATER)));
    }

    /** A curator swaps a group's picture for an upload, then removes it; a bad key and an unknown group are refused. */
    @Test
    void aCuratorChangesAGroupsPicture() throws Exception {
        RequestPostProcessor curator = staff("curator@desk.test", "EDITOR", "PERM_TAXONOMY_MANAGE");
        String water = waterId();
        String upload = "interest/" + UUID.randomUUID() + "/new-water.png";

        mvc.perform(put("/api/v1/interest-groups/" + water).with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coverKey\":\"" + upload + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Water"))
                .andExpect(jsonPath("$.coverKey").value(upload));
        mvc.perform(get("/api/v1/interest-groups"))
                .andExpect(jsonPath("$[3].coverKey").value(upload));

        /* Null keeps the picture; empty removes it. */
        mvc.perform(put("/api/v1/interest-groups/" + water).with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.coverKey").value(upload));
        mvc.perform(put("/api/v1/interest-groups/" + water).with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coverKey\":\"\"}"))
                .andExpect(jsonPath("$.coverKey", nullValue()))
                .andExpect(jsonPath("$.coverUrl", nullValue()));

        mvc.perform(put("/api/v1/interest-groups/" + water).with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coverKey\":\"not a key\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/interest-groups/" + UUID.randomUUID()).with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coverKey\":\"" + upload + "\"}"))
                .andExpect(status().isNotFound());
    }

    /** Everyone reads; only a taxonomy manager changes a picture. */
    @Test
    void readingIsPublicAndChangingIsTheTaxonomyManagers() throws Exception {
        mvc.perform(get("/api/v1/interest-groups")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/interest-groups/" + waterId()).with(staff("partner@hotel.test", "PARTNER", "PERM_LISTING_EDIT_OWN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coverKey\":\"\"}"))
                .andExpect(status().isForbidden());
    }

    private String waterId() throws Exception {
        String body = mvc.perform(get("/api/v1/interest-groups")).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[3].id");
    }

    private static RequestPostProcessor staff(String email, String... grants) {
        return jwt().jwt(token -> token.subject("subject-" + email).claim("email", email))
                .authorities(Arrays.stream(grants)
                        .map(grant -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + grant))
                        .toList());
    }
}
