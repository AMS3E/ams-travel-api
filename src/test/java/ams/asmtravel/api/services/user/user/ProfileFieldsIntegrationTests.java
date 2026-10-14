package ams.asmtravel.api.services.user.user;

import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.support.TestCatalogue;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static ams.asmtravel.api.support.Callers.customer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The Profile screen's home province and completion score, and the Preferences screen's distance unit. */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class ProfileFieldsIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired TestCatalogue catalogue;

    @Test
    void aPersonNamesTheirHomeProvinceAndSeesHowCompleteTheirProfileIs() throws Exception {
        Province province = catalogue.province();
        entityManager.flush();
        entityManager.clear();
        RequestPostProcessor me = customer(UUID.randomUUID().toString(), "home@profile.test");

        // Name and email from the sign-in: two of five fields.
        mvc.perform(get("/api/v1/users/me").with(me))
                .andExpect(jsonPath("$.profileCompletion").value(40))
                .andExpect(jsonPath("$.homeProvinceId").isEmpty());
        mvc.perform(put("/api/v1/users/me").with(me).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Dara Chan\",\"phone\":\"+855 12 345 678\",\"homeProvinceId\":\"" + province.getId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeProvinceId").value(province.getId().toString()))
                .andExpect(jsonPath("$.homeProvinceName").value(province.getName()))
                .andExpect(jsonPath("$.phone").value("+855 12 345 678"))
                .andExpect(jsonPath("$.profileCompletion").value(80));
        mvc.perform(put("/api/v1/users/me").with(me).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Dara Chan\",\"homeProvinceId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
        // The whole profile is sent each time: leaving the province out clears it.
        mvc.perform(put("/api/v1/users/me").with(me).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Dara Chan\"}"))
                .andExpect(jsonPath("$.homeProvinceId").isEmpty())
                .andExpect(jsonPath("$.homeProvinceName").isEmpty())
                .andExpect(jsonPath("$.profileCompletion").value(60));
    }

    @Test
    void theDistanceUnitIsKeptWithThePreferences() throws Exception {
        RequestPostProcessor me = customer(UUID.randomUUID().toString(), "miles@profile.test");
        mvc.perform(get("/api/v1/preferences/me").with(me))
                .andExpect(jsonPath("$.distanceUnit").value("KM"));
        mvc.perform(put("/api/v1/preferences/me").with(me).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredLanguage\":\"en\",\"preferredCurrency\":\"USD\",\"notificationEmail\":true,"
                                + "\"notificationPush\":true,\"theme\":\"SYSTEM\",\"distanceUnit\":\"MI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanceUnit").value("MI"));
        // Left out, kilometres, as before the field existed.
        mvc.perform(put("/api/v1/preferences/me").with(me).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredLanguage\":\"en\",\"preferredCurrency\":\"USD\",\"notificationEmail\":true,"
                                + "\"notificationPush\":true,\"theme\":\"SYSTEM\"}"))
                .andExpect(jsonPath("$.distanceUnit").value("KM"));
        mvc.perform(put("/api/v1/preferences/me").with(me).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredLanguage\":\"en\",\"preferredCurrency\":\"USD\",\"notificationEmail\":true,"
                                + "\"notificationPush\":true,\"theme\":\"SYSTEM\",\"distanceUnit\":\"FURLONGS\"}"))
                .andExpect(status().isBadRequest());
    }
}
