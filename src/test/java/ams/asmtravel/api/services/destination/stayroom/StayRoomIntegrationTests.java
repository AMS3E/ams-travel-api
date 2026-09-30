package ams.asmtravel.api.services.destination.stayroom;

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
class StayRoomIntegrationTests {

    private static final String PIN = "\"latitude\":11.5564,\"longitude\":104.9282";
    private static final String PHOTO = "stay/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".jpg";

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired UserRepository users;

    /**
     * A partner furnishes their own hostel room by room: beds that say who a
     * room sleeps, a fixed list of what it has, and a price. The cheapest room
     * on sale is the listing's "from" price, whatever the stay form sends.
     */
    @Test
    void aPartnerFurnishesTheirOwnStayRoomByRoom() throws Exception {
        User owner = person("owner@hostel.test");
        RequestPostProcessor partner = staff("owner@hostel.test", "PARTNER", "PERM_LISTING_EDIT_OWN", "PERM_LISTING_DELETE_OWN");
        String stay = id(mvc.perform(post("/api/v1/stays").with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riverside Hostel\",\"type\":\"HOSTEL\",\"priceFrom\":99," + PIN + "}"))
                .andExpect(status().isCreated()));

        String dorm = id(room(partner, "{\"stayId\":\"" + stay + "\",\"name\":\"Bed in 6-Bed Mixed Dorm\",\"roomType\":\"DORM_BED\","
                + "\"roomCount\":6,\"bunkBeds\":3,\"maxAdults\":1,\"bathroom\":\"SHARED\","
                + "\"amenities\":[\"fan\",\"mosquito-net\",\"hot-water\"],\"pricePerNight\":8,\"freeCancellationDays\":1,"
                + "\"photoKeys\":[\"" + PHOTO + "\"]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sleeps").value(6))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.bathroom").value("SHARED"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.photoUrls[0]", endsWith(PHOTO))));
        String twin = id(room(partner, "{\"stayId\":\"" + stay + "\",\"name\":\"Private Twin\",\"roomType\":\"TWIN\","
                + "\"singleBeds\":2,\"view\":\"RIVER\",\"pricePerNight\":22,\"breakfastIncluded\":true,"
                + "\"highlight\":\"Corner room over the river\",\"hasWindow\":false,\"extraBeds\":1,\"extraBedPrice\":5,"
                + "\"highSeasonPrice\":30,\"taxesIncluded\":false,\"minNights\":2,\"paymentTerms\":\"DEPOSIT\","
                + "\"amenities\":[\"shower\",\"towels\"]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sortOrder").value(1))
                .andExpect(jsonPath("$.maxAdults").value(2))
                .andExpect(jsonPath("$.view").value("RIVER"))
                .andExpect(jsonPath("$.highlight").value("Corner room over the river"))
                .andExpect(jsonPath("$.hasWindow").value(false))
                .andExpect(jsonPath("$.extraBedPrice").value(5.0))
                .andExpect(jsonPath("$.highSeasonPrice").value(30.0))
                .andExpect(jsonPath("$.taxesIncluded").value(false))
                .andExpect(jsonPath("$.minNights").value(2))
                .andExpect(jsonPath("$.paymentTerms").value("DEPOSIT")));

        mvc.perform(get("/api/v1/stays/" + stay).with(partner)).andExpect(jsonPath("$.priceFrom").value(8.0));
        mvc.perform(put("/api/v1/stays/" + stay).with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riverside Hostel\",\"type\":\"HOSTEL\",\"priceFrom\":99," + PIN + "}"))
                .andExpect(jsonPath("$.priceFrom").value(8.0));

        // Off sale: the next cheapest sets the price.
        room(partner, "PUT", dorm, "{\"stayId\":\"" + stay + "\",\"name\":\"Bed in 6-Bed Mixed Dorm\",\"roomType\":\"DORM_BED\","
                + "\"bunkBeds\":3,\"pricePerNight\":8,\"isActive\":false}")
                .andExpect(jsonPath("$.isActive").value(false));
        mvc.perform(get("/api/v1/stays/" + stay).with(partner)).andExpect(jsonPath("$.priceFrom").value(22.0));

        // What a room must have, and who may write one.
        room(partner, "{\"stayId\":\"" + stay + "\",\"name\":\"Nowhere to Sleep\",\"roomType\":\"DOUBLE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Add at least one bed"));
        room(partner, "{\"stayId\":\"" + stay + "\",\"name\":\"Spa Room\",\"roomType\":\"DOUBLE\",\"doubleBeds\":1,\"amenities\":[\"jacuzzi\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("jacuzzi")));
        room(partner, "{\"name\":\"No Stay\",\"roomType\":\"DOUBLE\",\"doubleBeds\":1}").andExpect(status().isBadRequest());
        person("rival@hostel.test");
        room(staff("rival@hostel.test", "PARTNER", "PERM_LISTING_EDIT_OWN"),
                "{\"stayId\":\"" + stay + "\",\"name\":\"Not Mine\",\"roomType\":\"DOUBLE\",\"doubleBeds\":1}")
                .andExpect(status().isForbidden());
        room(customer(), "{\"stayId\":\"" + stay + "\",\"name\":\"Visitor\",\"roomType\":\"DOUBLE\",\"doubleBeds\":1}")
                .andExpect(status().isForbidden());

        // The public sees the rooms on sale in a published stay, and nothing of a draft.
        mvc.perform(get("/api/v1/stay-rooms").param("stayId", stay)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(put("/api/v1/stays/" + stay).with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riverside Hostel\",\"type\":\"HOSTEL\",\"ownerUserId\":\"" + owner.getId()
                                + "\",\"status\":\"PUBLISHED\"," + PIN + "}"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mvc.perform(get("/api/v1/stay-rooms").param("stayId", stay))
                .andExpect(jsonPath("$[*].name", contains("Private Twin")));
        mvc.perform(get("/api/v1/stay-rooms/" + dorm)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/stay-rooms").param("stayId", stay).with(partner)).andExpect(jsonPath("$", hasSize(2)));

        // A room stays with its listing; a deleted listing takes its rooms.
        String other = id(mvc.perform(post("/api/v1/stays").with(partner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Second House\",\"type\":\"GUESTHOUSE\"," + PIN + "}"))
                .andExpect(status().isCreated()));
        room(partner, "PUT", twin, "{\"stayId\":\"" + other + "\",\"name\":\"Private Twin\",\"roomType\":\"TWIN\",\"singleBeds\":2}")
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/stay-rooms/" + dorm).with(partner)).andExpect(status().isNoContent());
        nextRequest();
        mvc.perform(delete("/api/v1/stays/" + stay).with(editor())).andExpect(status().isNoContent());
        nextRequest();
        mvc.perform(get("/api/v1/stay-rooms/" + twin).with(editor())).andExpect(status().isNotFound());
    }

    /**
     * What a guest checks before booking, beyond the rooms: the house rules,
     * breakfast and parking, how to pay, the languages spoken and how to reach
     * the property. The lists are fixed, and a value off them is named.
     */
    @Test
    void aStayCarriesItsHouseRulesAndWaysToPay() throws Exception {
        String stay = id(mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mekong Guesthouse\",\"type\":\"GUESTHOUSE\"," + PIN
                                + ",\"checkInFrom\":\"14:00\",\"checkInUntil\":\"22:00\",\"checkOutFrom\":\"06:00\",\"checkOutUntil\":\"12:00\","
                                + "\"minCheckInAge\":18,\"childrenAllowed\":true,\"petsPolicy\":\"ON_REQUEST\",\"smokingPolicy\":\"DESIGNATED_AREAS\","
                                + "\"quietHours\":\"22:00-07:00\",\"breakfastOption\":\"EXTRA_CHARGE\",\"breakfastTypes\":[\"khmer\",\"asian\"],"
                                + "\"breakfastPrice\":4,\"parkingOption\":\"FREE\",\"paymentMethods\":[\"cash\",\"khqr\",\"aba-pay\"],"
                                + "\"languagesSpoken\":[\"km\",\"en\"],\"telegram\":\"@mekongguesthouse\",\"whatsapp\":\"+855 12 345 678\","
                                + "\"facebookUrl\":\"https://facebook.com/mekongguesthouse\",\"licenseNumber\":\"MOT-0001\",\"yearOpened\":2015}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.checkInUntil").value("22:00:00"))
                .andExpect(jsonPath("$.petsPolicy").value("ON_REQUEST"))
                .andExpect(jsonPath("$.partiesAllowed").value(false))
                .andExpect(jsonPath("$.breakfastTypes", contains("khmer", "asian")))
                .andExpect(jsonPath("$.paymentMethods", contains("cash", "khqr", "aba-pay")))
                .andExpect(jsonPath("$.languagesSpoken", contains("km", "en")))
                .andExpect(jsonPath("$.telegram").value("@mekongguesthouse"))
                .andExpect(jsonPath("$.yearOpened").value(2015)));
        mvc.perform(get("/api/v1/stays/" + stay).with(editor())).andExpect(jsonPath("$.licenseNumber").value("MOT-0001"));

        mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Card Only\",\"type\":\"HOTEL\"," + PIN + ",\"paymentMethods\":[\"bitcoin\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No such payment method: bitcoin"));

        // Each kind keeps what only it has: a hostel its dorms and pub crawls, a homestay its village life.
        mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lucky Backpackers\",\"type\":\"HOSTEL\"," + PIN
                                + ",\"specialties\":[\"female-dorms\",\"pod-beds\",\"pub-crawls\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specialties", contains("female-dorms", "pod-beds", "pub-crawls")));
        mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Village Home\",\"type\":\"HOMESTAY\"," + PIN
                                + ",\"specialties\":[\"stilt-house\",\"cook-with-hosts\",\"community-tourism\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specialties", contains("stilt-house", "cook-with-hosts", "community-tourism")));
        mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Moon Base\",\"type\":\"HOTEL\"," + PIN + ",\"specialties\":[\"helipad\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No such specialty: helipad"));
        mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Night Owl\",\"type\":\"HOTEL\"," + PIN + ",\"quietHours\":\"late\"}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * What a room's own page answers beyond its beds: how it is laid out, where
     * in the building it is and how it is cleaned, the cots and the children who
     * sleep free, and what a week or a month costs. A deposit share only keeps
     * its meaning while a deposit is what books the room.
     */
    @Test
    void aRoomTellsItsLayoutCotsAndLongerStayPrices() throws Exception {
        String stay = id(mvc.perform(post("/api/v1/stays").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Riverside Apartments\",\"type\":\"HOTEL\"," + PIN + "}"))
                .andExpect(status().isCreated()));

        String apartment = id(room(editor(), "{\"stayId\":\"" + stay + "\",\"name\":\"Two-Bedroom Apartment\",\"roomType\":\"APARTMENT\","
                + "\"queenBeds\":2,\"bedrooms\":2,\"livingRooms\":1,\"bathroomCount\":2,\"floor\":3,\"liftAccess\":true,"
                + "\"housekeeping\":\"WEEKLY\",\"cots\":1,\"cotPrice\":3,\"childrenFreeUnderAge\":6,"
                + "\"pricePerNight\":40,\"weeklyPrice\":250,\"monthlyPrice\":900,"
                + "\"paymentTerms\":\"DEPOSIT\",\"depositPercent\":30,\"amenities\":[\"kitchen\",\"usb-sockets\",\"clothes-rack\"]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bedrooms").value(2))
                .andExpect(jsonPath("$.livingRooms").value(1))
                .andExpect(jsonPath("$.bathroomCount").value(2))
                .andExpect(jsonPath("$.floor").value(3))
                .andExpect(jsonPath("$.liftAccess").value(true))
                .andExpect(jsonPath("$.housekeeping").value("WEEKLY"))
                .andExpect(jsonPath("$.cotPrice").value(3.0))
                .andExpect(jsonPath("$.childrenFreeUnderAge").value(6))
                .andExpect(jsonPath("$.weeklyPrice").value(250.0))
                .andExpect(jsonPath("$.monthlyPrice").value(900.0))
                .andExpect(jsonPath("$.depositPercent").value(30)));

        // Paying at the property leaves no deposit to take, and a room with no cot no cot price.
        room(editor(), "PUT", apartment, "{\"stayId\":\"" + stay + "\",\"name\":\"Two-Bedroom Apartment\",\"roomType\":\"APARTMENT\","
                + "\"queenBeds\":2,\"bedrooms\":2,\"cots\":0,\"cotPrice\":3,\"paymentTerms\":\"PAY_AT_PROPERTY\",\"depositPercent\":30}")
                .andExpect(jsonPath("$.depositPercent").doesNotExist())
                .andExpect(jsonPath("$.cotPrice").doesNotExist())
                .andExpect(jsonPath("$.livingRooms").value(0));

        // A room that says nothing of its layout is one bedroom, one bathroom, stairs and no cot.
        room(editor(), "{\"stayId\":\"" + stay + "\",\"name\":\"Standard Double\",\"roomType\":\"DOUBLE\",\"doubleBeds\":1}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bedrooms").value(1))
                .andExpect(jsonPath("$.bathroomCount").value(1))
                .andExpect(jsonPath("$.cots").value(0))
                .andExpect(jsonPath("$.liftAccess").value(false))
                .andExpect(jsonPath("$.floor").doesNotExist())
                .andExpect(jsonPath("$.housekeeping").doesNotExist());

        room(editor(), "{\"stayId\":\"" + stay + "\",\"name\":\"Penthouse\",\"roomType\":\"SUITE\",\"kingBeds\":1,\"depositPercent\":120}")
                .andExpect(status().isBadRequest());
    }

    /* ---------------------------------------------------------------------- */

    private ResultActions room(RequestPostProcessor caller, String json) throws Exception {
        return mvc.perform(post("/api/v1/stay-rooms").with(caller).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions room(RequestPostProcessor caller, String method, String id, String json) throws Exception {
        return mvc.perform(put("/api/v1/stay-rooms/" + id).with(caller).contentType(MediaType.APPLICATION_JSON).content(json));
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
        return jwt().jwt(token -> token.subject("subject-visitor").claim("email", "visitor@room.test"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
