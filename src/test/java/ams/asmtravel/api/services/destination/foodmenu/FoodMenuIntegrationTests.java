package ams.asmtravel.api.services.destination.foodmenu;

import ams.asmtravel.api.services.user.user.domain.User;
import ams.asmtravel.api.services.user.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
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
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
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
class FoodMenuIntegrationTests {

    private static final String PIN = "\"latitude\":11.5564,\"longitude\":104.9282";
    private static final String PHOTO = "food/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".jpg";

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired UserRepository users;

    /**
     * A partner writes their menu dish by dish: what it is in both languages,
     * what is in it, how spicy it is and what it costs. The cheapest dish on
     * the menu is the listing's "from" price, whatever the listing form sent.
     */
    @Test
    void aPartnerWritesTheirMenuDishByDish() throws Exception {
        User owner = person("owner@kitchen.test");
        RequestPostProcessor partner = staff("owner@kitchen.test", "PARTNER", "PERM_LISTING_EDIT_OWN", "PERM_LISTING_DELETE_OWN");
        String food = id(mvc.perform(post("/api/v1/foods").with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mekong Kitchen\",\"type\":\"RESTAURANT\",\"priceFrom\":99," + PIN + "}"))
                .andExpect(status().isCreated()));

        String amok = id(dish(partner, "{\"foodId\":\"" + food + "\",\"name\":\"Fish amok\",\"localName\":\"អាម៉ុកត្រី\","
                + "\"pronunciation\":\"amok trey\",\"course\":\"MAIN\",\"price\":6.5,\"portion\":\"One bowl\",\"serves\":1,"
                + "\"spiceLevel\":1,\"labels\":[\"contains-shellfish\"],\"isSignature\":true,"
                + "\"description\":\"Steamed in banana leaf with coconut and noni.\",\"photoKeys\":[\"" + PHOTO + "\"]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.localName").value("អាម៉ុកត្រី"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.isSignature").value(true))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.photoUrls[0]", endsWith(PHOTO))));
        String coffee = id(dish(partner, "{\"foodId\":\"" + food + "\",\"name\":\"Iced Khmer coffee\",\"course\":\"COFFEE\","
                + "\"price\":1.5,\"labels\":[\"vegetarian\",\"contains-dairy\"],\"spiceLevel\":0}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sortOrder").value(1))
                .andExpect(jsonPath("$.serves").value(1))
                .andExpect(jsonPath("$.labels", contains("vegetarian", "contains-dairy"))));

        mvc.perform(get("/api/v1/foods/" + food).with(partner)).andExpect(jsonPath("$.priceFrom").value(1.5));
        mvc.perform(put("/api/v1/foods/" + food).with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mekong Kitchen\",\"type\":\"RESTAURANT\",\"priceFrom\":99," + PIN + "}"))
                .andExpect(jsonPath("$.priceFrom").value(1.5));

        // Off the menu: the next cheapest sets the price.
        dish(partner, "PUT", coffee, "{\"foodId\":\"" + food + "\",\"name\":\"Iced Khmer coffee\",\"course\":\"COFFEE\","
                + "\"price\":1.5,\"isActive\":false}")
                .andExpect(jsonPath("$.isActive").value(false));
        mvc.perform(get("/api/v1/foods/" + food).with(partner)).andExpect(jsonPath("$.priceFrom").value(6.5));

        // What a dish must have, and who may write one.
        dish(partner, "{\"foodId\":\"" + food + "\",\"name\":\"Mystery\",\"course\":\"MAIN\",\"labels\":[\"lucky\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("lucky")));
        dish(partner, "{\"name\":\"No Listing\",\"course\":\"MAIN\"}").andExpect(status().isBadRequest());
        person("rival@kitchen.test");
        dish(staff("rival@kitchen.test", "PARTNER", "PERM_LISTING_EDIT_OWN"),
                "{\"foodId\":\"" + food + "\",\"name\":\"Not Mine\",\"course\":\"MAIN\"}")
                .andExpect(status().isForbidden());
        dish(customer(), "{\"foodId\":\"" + food + "\",\"name\":\"Visitor\",\"course\":\"MAIN\"}")
                .andExpect(status().isForbidden());

        // The public sees the available dishes of a published listing, and nothing of a draft.
        mvc.perform(get("/api/v1/food-menu-items").param("foodId", food)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(put("/api/v1/foods/" + food).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mekong Kitchen\",\"type\":\"RESTAURANT\",\"ownerUserId\":\"" + owner.getId()
                                + "\",\"status\":\"PUBLISHED\"," + PIN + "}"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/food-menu-items").param("foodId", food))
                .andExpect(jsonPath("$[*].name", contains("Fish amok")));
        mvc.perform(get("/api/v1/food-menu-items/" + coffee)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/food-menu-items").param("foodId", food).with(partner)).andExpect(jsonPath("$", hasSize(2)));

        // A dish stays on its own menu; a deleted listing takes its dishes.
        String other = id(mvc.perform(post("/api/v1/foods").with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Second Stall\",\"type\":\"STREET_FOOD\"," + PIN + "}"))
                .andExpect(status().isCreated()));
        dish(partner, "PUT", amok, "{\"foodId\":\"" + other + "\",\"name\":\"Fish amok\",\"course\":\"MAIN\"}")
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/food-menu-items/" + coffee).with(partner)).andExpect(status().isNoContent());
        nextRequest();
        mvc.perform(delete("/api/v1/foods/" + food).with(editor())).andExpect(status().isNoContent());
        nextRequest();
        mvc.perform(get("/api/v1/food-menu-items/" + amok).with(editor())).andExpect(status().isNotFound());
    }

    /**
     * What a traveller checks before walking in: the cuisines, the meals of the
     * day, what the kitchen does for a diet, how a meal is had, who it suits,
     * how the bill is paid and how to reach it. The lists are fixed, and a
     * value off them is named.
     */
    @Test
    void aFoodListingSaysWhatItCooksAndHowYouEatThere() throws Exception {
        String food = id(mvc.perform(post("/api/v1/foods").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riverside Grill\",\"type\":\"RESTAURANT\"," + PIN
                                + ",\"cuisines\":[\"khmer\",\"bbq-grill\",\"seafood\"],\"mealsServed\":[\"lunch\",\"dinner\"],"
                                + "\"dietary\":[\"vegetarian-options\",\"less-spicy-on-request\"],"
                                + "\"serviceOptions\":[\"dine-in\",\"takeaway\",\"delivery\"],"
                                + "\"amenities\":[\"riverside\",\"air-conditioned\",\"kids-menu\",\"motorbike-parking\"],"
                                + "\"goodFor\":[\"families\",\"sunset\"],\"menuLanguages\":[\"km\",\"en\"],"
                                + "\"deliveryPartners\":[\"foodpanda\",\"nham24\"],\"paymentMethods\":[\"cash\",\"khqr\"],"
                                + "\"languagesSpoken\":[\"km\",\"en\"],\"alcohol\":\"BEER_WINE\",\"parkingOption\":\"STREET\","
                                + "\"reservations\":\"RECOMMENDED\",\"seatingCapacity\":80,\"averageMealPrice\":8.5,"
                                + "\"telegram\":\"@riversidegrill\",\"whatsapp\":\"+855 12 345 678\","
                                + "\"facebookUrl\":\"https://facebook.com/riversidegrill\",\"yearOpened\":2018,"
                                + "\"openingHours\":{\"mon\":\"10:00-22:00\",\"tue\":\"closed\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuisines", contains("khmer", "bbq-grill", "seafood")))
                .andExpect(jsonPath("$.mealsServed", contains("lunch", "dinner")))
                .andExpect(jsonPath("$.serviceOptions", contains("dine-in", "takeaway", "delivery")))
                .andExpect(jsonPath("$.goodFor", contains("families", "sunset")))
                .andExpect(jsonPath("$.deliveryPartners", contains("foodpanda", "nham24")))
                .andExpect(jsonPath("$.alcohol").value("BEER_WINE"))
                .andExpect(jsonPath("$.reservations").value("RECOMMENDED"))
                .andExpect(jsonPath("$.seatingCapacity").value(80))
                .andExpect(jsonPath("$.averageMealPrice").value(8.5))
                .andExpect(jsonPath("$.yearOpened").value(2018)));
        mvc.perform(get("/api/v1/foods/" + food).with(editor()))
                .andExpect(jsonPath("$.telegram").value("@riversidegrill"))
                .andExpect(jsonPath("$.menuLanguages", contains("km", "en")));

        mvc.perform(post("/api/v1/foods").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Martian Diner\",\"type\":\"RESTAURANT\"," + PIN + ",\"cuisines\":[\"martian\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No such cuisine: martian"));
        mvc.perform(post("/api/v1/foods").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Helipad Grill\",\"type\":\"RESTAURANT\"," + PIN + ",\"amenities\":[\"helipad\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No such facility: helipad"));

        // A cafe keeps what only a cafe has: its coffee, its beans, its milk.
        mvc.perform(post("/api/v1/foods").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mekong Morning Coffee\",\"type\":\"CAFE\"," + PIN
                                + ",\"specialties\":[\"khmer-iced-coffee\",\"mondulkiri-beans\",\"oat-milk\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specialties", contains("khmer-iced-coffee", "mondulkiri-beans", "oat-milk")));
        // …and a restaurant what only a restaurant has: how it serves, cooks and hosts.
        mvc.perform(post("/api/v1/foods").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Royal Riverside Table\",\"type\":\"RESTAURANT\"," + PIN
                                + ",\"specialties\":[\"cook-at-table\",\"royal-khmer-recipes\",\"banquets\",\"dress-smart-casual\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specialties", contains("cook-at-table", "royal-khmer-recipes", "banquets", "dress-smart-casual")));
        mvc.perform(post("/api/v1/foods").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Odd Cafe\",\"type\":\"CAFE\"," + PIN + ",\"specialties\":[\"unicorn-latte\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No such specialty: unicorn-latte"));
    }

    /* ---------------------------------------------------------------------- */

    private ResultActions dish(RequestPostProcessor caller, String json) throws Exception {
        return mvc.perform(post("/api/v1/food-menu-items").with(caller).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions dish(RequestPostProcessor caller, String method, String id, String json) throws Exception {
        return mvc.perform(put("/api/v1/food-menu-items/" + id).with(caller).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private void nextRequest() {
        entityManager.flush();
        entityManager.clear();
    }

    private User person(String email) {
        User user = new User();
        user.setFullName("Listing Person");
        user.setEmail(email);
        return users.save(user);
    }

    private static RequestPostProcessor staff(String email, String... grants) {
        return jwt().jwt(token -> token.subject("subject-" + email).claim("email", email))
                .authorities(Arrays.stream(grants)
                        .map(grant -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + grant))
                        .toList());
    }

    private static RequestPostProcessor editor() {
        return staff("editor@desk.test", "EDITOR", "PERM_DESTINATION_EDIT_ANY", "PERM_DESTINATION_UNPUBLISH",
                "PERM_DESTINATION_DRAFT_CREATE", "PERM_DESTINATION_DELETE");
    }

    private static RequestPostProcessor customer() {
        return jwt().jwt(token -> token.subject("subject-diner").claim("email", "visitor@menu.test"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
