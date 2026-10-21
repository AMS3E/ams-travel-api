package ams.asmtravel.api.services.destination.activity;

import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.support.TestCatalogue;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static ams.asmtravel.api.support.Callers.customer;
import static ams.asmtravel.api.support.Callers.editor;
import static ams.asmtravel.api.support.Callers.owner;
import static ams.asmtravel.api.support.Callers.staff;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Activities & Experiences, the fourth kind of destination (2026-10-21): written like an attraction
 * with a type and a contact, run by a partner like a stay, and a destination to every other service:
 * search, suggestions, cards, reviews, favorites, verification, check-in codes.
 */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class ActivityIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired TestCatalogue catalogue;

    private static RequestPostProcessor partner(String email) {
        return staff(email, "PARTNER", "PERM_LISTING_EDIT_OWN", "PERM_LISTING_DELETE_OWN", "PERM_MEDIA_UPLOAD",
                "PERM_PUBLISH_REQUEST_SUBMIT", "PERM_PUBLISH_REQUEST_VIEW_OWN");
    }

    private static String cruise(Province province, String more) {
        return "{\"name\":\"Kampi Dolphin Cruise\",\"type\":\"CRUISE\",\"provinceId\":\"" + province.getId() + "\","
                + "\"latitude\":12.556,\"longitude\":106.033,\"address\":\"Kampi landing, Kratie\","
                + "\"phone\":\"+855 72 555 3390\",\"contactEmail\":\"boats@kampi.test\",\"website\":\"https://kampi.test\","
                + "\"telegram\":\"@kampicruise\",\"whatsapp\":\"+855 10 555 339\",\"facebookUrl\":\"https://facebook.com/kampi.test\","
                + "\"slogan\":\"The engine stops, and then you hear them breathe.\",\"sloganBy\":\"a family from Phnom Penh\","
                + "\"description\":\"Two hours on the river to the dolphin pool.\"" + more + "}";
    }

    @Test
    void anEditorWritesAnActivityWithItsTypeAndContactAndItReachesEveryService() throws Exception {
        Province province = catalogue.province();
        String id = id(mvc.perform(post("/api/v1/activities").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content(cruise(province, "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.type").value("CRUISE"))
                .andExpect(jsonPath("$.slug").value("kampi-dolphin-cruise"))
                .andExpect(jsonPath("$.phone").value("+855 72 555 3390"))
                .andExpect(jsonPath("$.telegram").value("@kampicruise"))
                .andExpect(jsonPath("$.provinceId").value(province.getId().toString()))
                .andExpect(jsonPath("$.missingKhmer", containsInAnyOrder("nameKm", "descriptionKm", "sloganKm", "sloganByKm", "addressKm"))));

        // The type and the province are required; a zone is not a tourism region.
        mvc.perform(post("/api/v1/activities").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No type\",\"provinceId\":\"" + province.getId() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("type")));
        mvc.perform(post("/api/v1/activities").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No province\",\"type\":\"KAYAK\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("provinceId")));
        mvc.perform(post("/api/v1/activities").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content(cruise(province, ",\"tourismRegionIds\":[\"" + province.getRegion().getId() + "\"]")))
                .andExpect(status().is4xxClientError());

        // A draft is the back office's alone.
        mvc.perform(get("/api/v1/activities/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/activities/" + id).with(editor())).andExpect(status().isOk());

        // An editor publishes it outright; then the app sees it everywhere.
        mvc.perform(put("/api/v1/activities/" + id).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content(cruise(province, ",\"status\":\"PUBLISHED\",\"nameKm\":\"ជិះទូកមើលផ្សោតកំពី\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.nameKm").value("ជិះទូកមើលផ្សោតកំពី"))
                .andExpect(jsonPath("$.missingKhmer", containsInAnyOrder("descriptionKm", "sloganKm", "sloganByKm", "addressKm")));
        mvc.perform(get("/api/v1/activities/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactEmail").value("boats@kampi.test"));
        mvc.perform(get("/api/v1/activities").param("slug", "kampi-dolphin-cruise"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/v1/search").param("type", "ACTIVITY").param("q", "dolphin"))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].type").value("ACTIVITY"))
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.content[0].provinceId").value(province.getId().toString()))
                .andExpect(jsonPath("$.content[0].verified").value(false));
        mvc.perform(get("/api/v1/search").param("type", "ACTIVITY").param("provinceIds", province.getId().toString()))
                .andExpect(jsonPath("$.content", hasSize(1)));
        mvc.perform(get("/api/v1/search").param("type", "ACTIVITY").param("q", "dolphin").param("lang", "km"))
                .andExpect(jsonPath("$.content[0].name").value("ជិះទូកមើលផ្សោតកំពី"))
                .andExpect(jsonPath("$.content[0].untranslated", hasItem("provinceName")));
        mvc.perform(get("/api/v1/search/suggest").param("q", "kampi"))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].kind").value(hasItem("ACTIVITY")));
        mvc.perform(get("/api/v1/search/by-province").param("type", "ACTIVITY").param("perProvince", "3"))
                .andExpect(jsonPath("$[?(@.provinceId=='" + province.getId() + "')].totalElements").value(hasItem(1)));
        mvc.perform(get("/api/v1/popular").param("type", "ACTIVITY").param("limit", "5"))
                .andExpect(jsonPath("$.items[?(@.id=='" + id + "')].type").value(hasItem("ACTIVITY")));

        // Reviews, a save, a verification and a check-in code take an activity like any destination.
        mvc.perform(post("/api/v1/reviews").with(customer("rider@visit.test")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"ACTIVITY\",\"targetId\":\"" + id + "\",\"rating\":5,\"content\":\"They came right up to the boat\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/favorites").with(customer("rider@visit.test")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"ACTIVITY\",\"targetId\":\"" + id + "\"}"))
                .andExpect(status().is2xxSuccessful());
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + id + "\",\"verified\":true,\"basis\":\"OFFICIAL_SITE\",\"reference\":\"Kratie tourism office\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("ACTIVITY"))
                .andExpect(jsonPath("$.destinationName").value("Kampi Dolphin Cruise"));
        mvc.perform(post("/api/v1/stamps/codes").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"ACTIVITY\",\"targetId\":\"" + id + "\"}"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.code").isNotEmpty());
        mvc.perform(get("/api/v1/search/cards").param("type", "ACTIVITY").param("ids", id))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].reviewCount").value(1))
                .andExpect(jsonPath("$[0].averageRating").value(5.0))
                .andExpect(jsonPath("$[0].verified").value(true))
                .andExpect(jsonPath("$[0].basis").value("OFFICIAL_SITE"));
        mvc.perform(get("/api/v1/admin/destination-stats/" + id).with(owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.place.type").value("ACTIVITY"));

        // Deleted, it is gone from the cards and the reviews with it.
        mvc.perform(delete("/api/v1/activities/" + id).with(editor())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/search/cards").param("type", "ACTIVITY").param("ids", id))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/v1/reviews").param("targetType", "ACTIVITY").param("targetId", id))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void aPartnerListsTheirOwnActivityAndADeskPublishesIt() throws Exception {
        Province province = catalogue.province();
        RequestPostProcessor partner = partner("boats@kampi.test");
        String id = id(mvc.perform(post("/api/v1/activities").with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content(cruise(province, "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.ownerUserId").isNotEmpty()));

        // Only an editor publishes; another partner cannot touch it.
        mvc.perform(put("/api/v1/activities/" + id).with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content(cruise(province, ",\"status\":\"PUBLISHED\"")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/activities/" + id).with(partner("stranger@kampi.test")).contentType(MediaType.APPLICATION_JSON)
                        .content(cruise(province, "")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/activities").with(partner("stranger@kampi.test")))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]", hasSize(1)));

        // Sent to the desk, approved and published: live, with the partner still its owner.
        String request = id(mvc.perform(post("/api/v1/publish-requests").with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"ACTIVITY\",\"targetId\":\"" + id + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("ACTIVITY"))
                .andExpect(jsonPath("$.desk").value("PARTNER")));
        mvc.perform(post("/api/v1/publish-requests/" + request + "/approve").with(owner())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/v1/publish-requests/" + request + "/publish").with(owner()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/activities/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.ownerUserId").isNotEmpty());
        mvc.perform(get("/api/v1/destination-verifications/queue").with(owner()))
                .andExpect(jsonPath("$[?(@.destinationId=='" + id + "')].targetType").value(hasItem("ACTIVITY")))
                .andExpect(jsonPath("$[?(@.destinationId=='" + id + "')].desk").value(hasItem("PARTNER")));

        // Once live, the partner may still edit but not take it down.
        mvc.perform(put("/api/v1/activities/" + id).with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content(cruise(province, ",\"phone\":\"+855 72 555 0000\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(delete("/api/v1/activities/" + id).with(partner)).andExpect(status().isForbidden());
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
