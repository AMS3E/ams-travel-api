package ams.asmtravel.api.services.publishing;

import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.province.repository.ProvinceRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static ams.asmtravel.api.support.Callers.editor;
import static ams.asmtravel.api.support.Callers.staff;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A partner lists a stay or a place to eat as a draft and sends it for review;
 * it goes to the BDO desk — not the editors' — and goes live when a BDO (or an
 * administrator) approves it, as a partner's attraction does. A contributor
 * does the same, and their listing goes to the editors' desk instead.
 */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class ListingPublishingIntegrationTests {

    private static final String PIN = "\"latitude\":11.5564,\"longitude\":104.9282";

    @Autowired MockMvc mvc;
    @Autowired ProvinceRepository provinces;

    private static RequestPostProcessor partner(String email) {
        return staff(email, "PARTNER", "PERM_LISTING_EDIT_OWN", "PERM_LISTING_DELETE_OWN", "PERM_MEDIA_UPLOAD",
                "PERM_PUBLISH_REQUEST_SUBMIT", "PERM_PUBLISH_REQUEST_VIEW_OWN");
    }

    private static RequestPostProcessor contributor(String email) {
        return staff(email, "CONTRIBUTOR", "PERM_DESTINATION_DRAFT_CREATE", "PERM_DESTINATION_DRAFT_EDIT_OWN",
                "PERM_DESTINATION_DRAFT_DELETE_OWN", "PERM_LISTING_EDIT_OWN", "PERM_LISTING_DELETE_OWN",
                "PERM_MEDIA_UPLOAD", "PERM_PUBLISH_REQUEST_SUBMIT", "PERM_PUBLISH_REQUEST_VIEW_OWN");
    }

    /** An editor as the realm grants one: they publish directly, and submit their own content. */
    private static RequestPostProcessor editorDesk() {
        return staff("editor@own.test", "EDITOR", "PERM_DESTINATION_EDIT_ANY", "PERM_DESTINATION_PUBLISH",
                "PERM_DESTINATION_UNPUBLISH", "PERM_CONTRIBUTOR_PUBLISH_APPROVE", "PERM_MEDIA_UPLOAD",
                "PERM_PUBLISH_REQUEST_SUBMIT", "PERM_PUBLISH_REQUEST_VIEW_OWN");
    }

    private static RequestPostProcessor bdo() {
        return staff("bdo@desk.test", "BDO", "PERM_DESTINATION_EDIT_ANY", "PERM_PARTNER_PUBLISH_APPROVE",
                "PERM_PUBLISH_REQUEST_SUBMIT", "PERM_PUBLISH_REQUEST_VIEW_OWN");
    }

    @Test
    void aPartnersStayGoesToTheBdoDeskAndLiveOnApproval() throws Exception {
        RequestPostProcessor partner = partner("owner@guesthouse.test");
        String stay = id(mvc.perform(post("/api/v1/stays").with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Memo Guesthouse\",\"nameKm\":\"ផ្ទះសំណាក់មេម៉ូ\",\"type\":\"GUESTHOUSE\"," + PIN + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT")));
        mvc.perform(get("/api/v1/stays/" + stay)).andExpect(status().isNotFound());

        String request = id(submit(partner, "{\"targetType\":\"STAY\",\"targetId\":\"" + stay + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("STAY"))
                .andExpect(jsonPath("$.targetId").value(stay))
                .andExpect(jsonPath("$.targetName").value("Memo Guesthouse"))
                .andExpect(jsonPath("$.requestedByRole").value("PARTNER"))
                .andExpect(jsonPath("$.desk").value("PARTNER"))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW")));
        submit(partner, "{\"targetType\":\"STAY\",\"targetId\":\"" + stay + "\"}").andExpect(status().isConflict());

        // The partner tracks it among their own requests; a BDO has it in their queue, an editor does not.
        mvc.perform(get("/api/v1/publish-requests/mine").with(partner))
                .andExpect(jsonPath("$[0].id").value(request))
                .andExpect(jsonPath("$[0].targetType").value("STAY"));
        mvc.perform(get("/api/v1/publish-requests/pending").with(bdo())).andExpect(jsonPath("$[*].id", hasItem(request)));
        mvc.perform(get("/api/v1/publish-requests/pending").with(editor())).andExpect(jsonPath("$[*].id", not(hasItem(request))));
        mvc.perform(post("/api/v1/publish-requests/" + request + "/approve").with(editor())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/publish-requests/" + request + "/approve").with(bdo())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Welcome aboard\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        // Approved is not yet live: publishing it is its own step.
        mvc.perform(get("/api/v1/stays/" + stay)).andExpect(status().isNotFound());
        submit(partner, "{\"targetType\":\"STAY\",\"targetId\":\"" + stay + "\"}").andExpect(status().isConflict());
        mvc.perform(post("/api/v1/publish-requests/" + request + "/publish").with(bdo()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());
        mvc.perform(get("/api/v1/stays/" + stay))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        submit(partner, "{\"targetType\":\"STAY\",\"targetId\":\"" + stay + "\"}").andExpect(status().isConflict());

        // Taking it off needs the unpublish grant; after that it can be put forward again, as a new request.
        mvc.perform(post("/api/v1/publish-requests/" + request + "/unpublish").with(bdo())).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/publish-requests/" + request + "/unpublish").with(editorDesk()))
                .andExpect(jsonPath("$.status").value("UNPUBLISHED"));
        mvc.perform(get("/api/v1/stays/" + stay)).andExpect(status().isNotFound());
        String again = id(submit(partner, "{\"targetType\":\"STAY\",\"targetId\":\"" + stay + "\"}").andExpect(status().isCreated()));
        mvc.perform(get("/api/v1/publish-requests/history").with(partner).param("targetType", "STAY").param("targetId", stay))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(again))
                .andExpect(jsonPath("$[1].status").value("UNPUBLISHED"));
        mvc.perform(get("/api/v1/publish-requests/current").with(partner).param("targetType", "STAY").param("targetId", stay))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
    }

    /**
     * A stay or a place to eat waits for its Khmer however it would go live: approved from the desk, published
     * by an editor, or saved straight as published. Its Khmer then shows on the Khmer cards.
     */
    @Test
    void staysAndFoodPlacesGoLiveOnlyWithTheirKhmer() throws Exception {
        RequestPostProcessor partner = partner("owner@khmer.test");
        String english = "\"name\":\"Lotus Guesthouse\",\"type\":\"GUESTHOUSE\",\"slogan\":\"Quiet nights\"," + PIN;
        String stay = id(mvc.perform(post("/api/v1/stays").with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + english + "}"))
                .andExpect(jsonPath("$.missingKhmer", contains("nameKm", "sloganKm"))));
        String request = id(submit(partner, "{\"targetType\":\"STAY\",\"targetId\":\"" + stay + "\"}").andExpect(status().isCreated()));
        mvc.perform(post("/api/v1/publish-requests/" + request + "/approve").with(bdo())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.missingKhmer", contains("nameKm", "sloganKm")));

        mvc.perform(put("/api/v1/stays/" + stay).with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + english + ",\"nameKm\":\"ផ្ទះសំណាក់ផ្កាឈូក\",\"sloganKm\":\"យប់ស្ងាត់\"}"))
                .andExpect(jsonPath("$.missingKhmer", hasSize(0)));
        mvc.perform(post("/api/v1/publish-requests/" + request + "/approve").with(bdo())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/v1/publish-requests/" + request + "/publish").with(bdo()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/search").param("type", "STAY").param("lang", "km").param("q", "ផ្កាឈូក"))
                .andExpect(jsonPath("$.content[0].name").value("ផ្ទះសំណាក់ផ្កាឈូក"))
                .andExpect(jsonPath("$.content[0].untranslated", not(hasItem("name"))));

        // An editor saving a food place straight as published is held to the same rule, and nothing is saved.
        String food = "\"name\":\"Lotus Noodles\",\"type\":\"RESTAURANT\",\"status\":\"PUBLISHED\"," + PIN;
        mvc.perform(post("/api/v1/foods").with(editorDesk()).contentType(MediaType.APPLICATION_JSON).content("{" + food + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.missingKhmer", contains("nameKm")));
        mvc.perform(get("/api/v1/search").param("type", "FOOD").param("q", "lotus")).andExpect(jsonPath("$.content", hasSize(0)));
        mvc.perform(post("/api/v1/foods").with(editorDesk()).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + food + ",\"nameKm\":\"គុយទាវផ្កាឈូក\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.nameKm").value("គុយទាវផ្កាឈូក"));
    }

    /** A contributor's attraction is not approved while its English has no Khmer beside it. */
    @Test
    void anAttractionIsApprovedOnlyWithItsKhmer() throws Exception {
        Province province = new Province();
        province.setName("Kampot");
        province.setSlug("kampot-khmer-test");
        province = provinces.save(province);
        RequestPostProcessor contributor = contributor("writer@places.test");
        String english = "\"name\":\"Bokor Hill Station\",\"provinceId\":\"" + province.getId() + "\"";
        String place = id(mvc.perform(post("/api/v1/destinations").with(contributor).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + english + "}"))
                .andExpect(status().isCreated()));
        String request = id(submit(contributor, "{\"targetType\":\"DESTINATION\",\"targetId\":\"" + place + "\"}")
                .andExpect(status().isCreated()));

        mvc.perform(post("/api/v1/publish-requests/" + request + "/approve").with(editor())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.missingKhmer", contains("nameKm")));
        mvc.perform(get("/api/v1/publish-requests/pending").with(editor())).andExpect(jsonPath("$[*].id", hasItem(request)));

        mvc.perform(put("/api/v1/destinations/" + place).with(contributor).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + english + ",\"nameKm\":\"ស្ថានីយ៍ភ្នំបូកគោ\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/publish-requests/" + request + "/approve").with(editor())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/v1/publish-requests/" + request + "/publish").with(editor()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/destinations/" + place)).andExpect(jsonPath("$.nameKm").value("ស្ថានីយ៍ភ្នំបូកគោ"));
    }

    @Test
    void aContributorsStayGoesToTheEditorDeskAndLiveOnApproval() throws Exception {
        RequestPostProcessor contributor = contributor("writer@stays.test");
        String stay = id(mvc.perform(post("/api/v1/stays").with(contributor).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Kampot River Homestay\",\"nameKm\":\"ផ្ទះស្នាក់មាត់ទន្លេកំពត\",\"type\":\"HOMESTAY\"," + PIN + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.ownerUserId").isNotEmpty()));

        String request = id(submit(contributor, "{\"targetType\":\"STAY\",\"targetId\":\"" + stay + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestedByRole").value("CONTRIBUTOR"))
                .andExpect(jsonPath("$.desk").value("CONTRIBUTOR")));

        // An editor has it in their queue, a BDO does not — and a BDO may not answer it.
        mvc.perform(get("/api/v1/publish-requests/pending").with(editor())).andExpect(jsonPath("$[*].id", hasItem(request)));
        mvc.perform(get("/api/v1/publish-requests/pending").with(bdo())).andExpect(jsonPath("$[*].id", not(hasItem(request))));
        mvc.perform(post("/api/v1/publish-requests/" + request + "/approve").with(bdo())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/publish-requests/" + request + "/approve").with(editor())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/v1/publish-requests/" + request + "/publish").with(editor()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/stays/" + stay))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // Publication is one-way for a contributor: the live stay is the editors' to change now.
        mvc.perform(put("/api/v1/stays/" + stay).with(contributor).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Kampot River Homestay and Cafe\",\"type\":\"HOMESTAY\"," + PIN + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/stays/" + stay).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Kampot River Homestay and Cafe\",\"type\":\"HOMESTAY\"," + PIN + "}"))
                .andExpect(status().isOk());
    }

    @Test
    void anEditorApprovesTheirOwnSubmissionRatherThanWaitingForAnAdministrator() throws Exception {
        RequestPostProcessor editor = editorDesk();
        String food = id(mvc.perform(post("/api/v1/foods").with(editor).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Editorial Kitchen\",\"nameKm\":\"ផ្ទះបាយវិចារណកថា\",\"type\":\"RESTAURANT\"," + PIN + "}"))
                .andExpect(status().isCreated()));
        String request = id(submit(editor, "{\"targetType\":\"FOOD\",\"targetId\":\"" + food + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestedByRole").value("EDITOR"))
                .andExpect(jsonPath("$.desk").value("STAFF")));

        // Another desk answers nothing of the staff's.
        mvc.perform(post("/api/v1/publish-requests/" + request + "/approve").with(bdo())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        // Their own is theirs to approve — but not to send back to themselves.
        mvc.perform(post("/api/v1/publish-requests/" + request + "/reject").with(editor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"more photos\",\"sendBack\":true}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/publish-requests/" + request + "/approve").with(editor)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/v1/publish-requests/" + request + "/publish").with(editor))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/foods/" + food))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void anAuthorWhoCannotPublishStillWaitsForTheirDesk() throws Exception {
        RequestPostProcessor partner = partner("owner@waiting.test");
        String stay = id(mvc.perform(post("/api/v1/stays").with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Waiting Guesthouse\",\"type\":\"GUESTHOUSE\"," + PIN + "}"))
                .andExpect(status().isCreated()));
        String request = id(submit(partner, "{\"targetType\":\"STAY\",\"targetId\":\"" + stay + "\"}").andExpect(status().isCreated()));
        mvc.perform(post("/api/v1/publish-requests/" + request + "/approve").with(partner)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aPartnersFoodListingIsPublishedTheSameWay() throws Exception {
        RequestPostProcessor partner = partner("owner@noodles.test");
        String food = id(mvc.perform(post("/api/v1/foods").with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Memo Noodles\",\"nameKm\":\"គុយទាវមេម៉ូ\",\"type\":\"RESTAURANT\"," + PIN + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT")));

        String request = id(submit(partner, "{\"targetType\":\"FOOD\",\"targetId\":\"" + food + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("FOOD"))
                .andExpect(jsonPath("$.desk").value("PARTNER")));
        mvc.perform(post("/api/v1/publish-requests/" + request + "/reject").with(bdo())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Add the opening hours\",\"sendBack\":true}"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
        // Sent again, the same request goes back to the desk.
        submit(partner, "{\"targetType\":\"FOOD\",\"targetId\":\"" + food + "\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(request))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));
        mvc.perform(post("/api/v1/publish-requests/" + request + "/approve").with(bdo())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/v1/publish-requests/" + request + "/publish").with(bdo()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/foods/" + food))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());
    }

    @Test
    void onlyItsOwnPartnerSendsAListingAndOnlyOneAtATime() throws Exception {
        RequestPostProcessor partner = partner("owner@villa.test");
        String stay = id(mvc.perform(post("/api/v1/stays").with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hill Villa\",\"type\":\"VILLA\"," + PIN + "}"))
                .andExpect(status().isCreated()));

        submit(partner("stranger@villa.test"), "{\"targetType\":\"STAY\",\"targetId\":\"" + stay + "\"}").andExpect(status().isForbidden());
        submit(partner, "{}").andExpect(status().isBadRequest());
        submit(partner, "{\"targetType\":\"CASTLE\",\"targetId\":\"" + stay + "\"}").andExpect(status().isBadRequest());
    }

    private ResultActions submit(RequestPostProcessor who, String body) throws Exception {
        return mvc.perform(post("/api/v1/publish-requests").with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }
}
