package ams.asmtravel.api.services.user.user.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A pre-flight that answers with the wrong headers fails silently in the browser
 * and looks like a dead API from the Next.js side, so the contract is pinned here
 * rather than left to be discovered in a dev tools network tab.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigurationIntegrationTests {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void preflightFromAConfiguredOriginIsAllowed() throws Exception {
        mockMvc.perform(options("/api/v1/regions")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    void credentialsAreAllowedSoTheOriginIsEchoedRatherThanWildcarded() throws Exception {
        mockMvc.perform(options("/api/v1/regions")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void preflightFromAnUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/regions")
                        .header("Origin", "https://not-our-frontend.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
