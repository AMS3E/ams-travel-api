package ams.asmtravel.api.services.taxonomy.category;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class CategoryIconIntegrationTests {

    private static final Path RESOURCES = Path.of("src/main/resources/ams/asmtravel/api/services");

    @Autowired MockMvc mvc;

    /** A curator gives a category an icon picture; a save without one keeps it, an empty one removes it. */
    @Test
    void aCategoryCarriesAnIconPicture() throws Exception {
        RequestPostProcessor curator = staff("curator@desk.test", "EDITOR", "PERM_TAXONOMY_MANAGE");
        String icon = "category/" + UUID.randomUUID() + "/stargazing.png";

        String body = mvc.perform(post("/api/v1/categories").with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Stargazing\",\"coverKey\":\"" + icon + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coverKey").value(icon))
                .andExpect(jsonPath("$.coverUrl", endsWith("/" + icon)))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(put("/api/v1/categories/" + id).with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Stargazing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverKey").value(icon));
        mvc.perform(put("/api/v1/categories/" + id).with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Stargazing\",\"coverKey\":\"\"}"))
                .andExpect(jsonPath("$.coverKey", nullValue()))
                .andExpect(jsonPath("$.coverUrl", nullValue()));
        mvc.perform(put("/api/v1/categories/" + id).with(curator).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Stargazing\",\"coverKey\":\"stargazing.svg\"}"))
                .andExpect(status().isBadRequest());
    }

    /** Every icon the category seed names is a seed image, so a fresh database gets all of them. */
    @Test
    void everySeededIconIsShipped() throws Exception {
        JsonNode rows = new ObjectMapper().readTree(RESOURCES.resolve("taxonomy/category/config/categories.json").toFile());
        List<String> missing = new ArrayList<>();
        int named = 0;
        for (JsonNode row : rows) {
            if (row.hasNonNull("image")) {
                named += 1;
                String file = row.get("image").asString();
                if (!Files.isRegularFile(RESOURCES.resolve("media/config/images").resolve(file))) {
                    missing.add(file);
                }
            }
        }
        assertThat(named).as("categories with an icon").isEqualTo(63);
        assertThat(missing).as("icons named by the seed but not shipped").isEmpty();
    }

    private static RequestPostProcessor staff(String email, String... grants) {
        return jwt().jwt(token -> token.subject("subject-" + email).claim("email", email))
                .authorities(Arrays.stream(grants)
                        .map(grant -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + grant))
                        .toList());
    }
}
