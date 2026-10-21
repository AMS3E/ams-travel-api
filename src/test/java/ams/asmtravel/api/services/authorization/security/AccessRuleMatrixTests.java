package ams.asmtravel.api.services.authorization.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static ams.asmtravel.api.support.Callers.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * The URL rules as a table: what an anonymous caller and a signed-in visitor
 * are told at the door, before any row is looked at. A rule that went missing
 * would let the "every GET is public" default publish something private, or
 * lock the public site out of something it reads.
 */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class AccessRuleMatrixTests {

    private static final String ID = "00000000-0000-0000-0000-00000000000a";

    @Autowired MockMvc mvc;

    /** Reads the public site makes: never 401 or 403, whatever the row says. */
    @ParameterizedTest
    @CsvSource({
            "/api/v1/destinations", "/api/v1/regions", "/api/v1/provinces", "/api/v1/categories", "/api/v1/tags",
            "/api/v1/interests", "/api/v1/corridors", "/api/v1/corridors/" + ID + "/itinerary",
            "/api/v1/corridor-destinations", "/api/v1/stays", "/api/v1/stay-rooms",
            "/api/v1/foods", "/api/v1/activities", "/api/v1/media?ownerType=FOOD", "/api/v1/media?ownerType=ACTIVITY", "/api/v1/media?ownerType=DESTINATION", "/api/v1/popular",
            "/api/v1/recommendations/for-you", "/api/v1/seasons", "/api/v1/seasons/current",
            "/api/v1/badges", "/api/v1/badges/" + ID,
            "/api/v1/badges/shared/" + ID, "/api/v1/destination-verifications/status",
            "/api/v1/historical-coverage/periods", "/api/v1/historical-coverage/monarchs", "/api/v1/historical-coverage/styles", "/api/v1/historical-coverage/religions",
            "/api/v1/destination-histories", "/api/v1/destination-monarchs", "/api/v1/ratings?targetType=DESTINATION&targetIds=" + ID,
            "/api/v1/reviews?targetType=DESTINATION&targetId=" + ID, "/api/v1/media?ownerType=STAY&ownerId=" + ID,
            "/api/v1/media?ownerType=CORRIDOR", "/api/v1/search/by-province", "/api/v1/badges/shared/" + ID + "/page",
            "/api/v1/popular?type=STAY", "/api/v1/stamps/scan/ABCD-EFGH-JKLM"
    })
    void anyoneReads(String path) throws Exception {
        assertThat(status(HttpMethod.GET, path, false)).isNotIn(401, 403);
    }

    /** What needs a signed-in account: 401 without one. */
    @ParameterizedTest
    @CsvSource({
            "GET, /api/v1/users/me", "GET, /api/v1/preferences/me", "GET, /api/v1/badges/me/progress",
            "PUT, /api/v1/badges/me/explorations/" + ID, "GET, /api/v1/badges/me/earned",
            "GET, /api/v1/stamps/me", "POST, /api/v1/stamps", "GET, /api/v1/stamps/codes?targetType=STAY&targetId=" + ID, "POST, /api/v1/stamps/codes",
            "GET, /api/v1/notifications", "GET, /api/v1/favorites", "GET, /api/v1/reviews/me", "POST, /api/v1/reviews",
            "GET, /api/v1/destination-verifications/queue",
            "GET, /api/v1/destination-verifications", "GET, /api/v1/admin/audit", "GET, /api/v1/admin/overview", "GET, /api/v1/admin/destination-stats", "GET, /api/v1/admin/destination-stats/" + ID,
            "GET, /api/v1/destinations/workspace", "GET, /api/v1/identities", "GET, /api/v1/user-roles",
            "GET, /api/v1/roles", "GET, /api/v1/permissions", "GET, /api/v1/staff", "POST, /api/v1/media/files",
            "GET, /api/v1/media/files/verification/" + ID + "/a.jpg"
    })
    void aSignInIsNeeded(String method, String path) throws Exception {
        assertThat(status(HttpMethod.valueOf(method), path, false)).isEqualTo(401);
    }

    /** What a visitor's account does not reach: the back office and every catalogue write. */
    @ParameterizedTest
    @CsvSource({
            "GET, /api/v1/destination-verifications/queue", "GET, /api/v1/admin/audit", "GET, /api/v1/admin/overview", "GET, /api/v1/admin/destination-stats", "GET, /api/v1/admin/destination-stats/" + ID,
            "GET, /api/v1/destinations/workspace", "POST, /api/v1/seasons", "POST, /api/v1/badges",
            "GET, /api/v1/stamps/codes?targetType=STAY&targetId=" + ID, "POST, /api/v1/stamps/codes", "DELETE, /api/v1/stamps/codes/" + ID,
            "POST, /api/v1/stamps/codes/" + ID + "/retire",
            "POST, /api/v1/historical-coverage/periods",
            "POST, /api/v1/historical-coverage/monarchs", "POST, /api/v1/destination-histories", "PUT, /api/v1/destination-monarchs/" + ID,
            "PUT, /api/v1/corridor-destinations/" + ID, "PUT, /api/v1/corridors/" + ID + "/itinerary",
            "POST, /api/v1/corridors/" + ID + "/route", "PATCH, /api/v1/corridors/" + ID + "/publish",
            "POST, /api/v1/corridors", "POST, /api/v1/destination-verifications",
            "DELETE, /api/v1/admin/audit", "POST, /api/v1/regions", "GET, /api/v1/media/files/verification/" + ID + "/a.jpg",
            "GET, /api/v1/identities", "GET, /api/v1/user-roles", "GET, /api/v1/roles", "GET, /api/v1/staff",
            "GET, /api/v1/reviews", "POST, /api/v1/stay-rooms", "PUT, /api/v1/stay-rooms/" + ID, "POST, /api/v1/activities", "DELETE, /api/v1/activities/" + ID
    })
    void aVisitorIsTurnedAway(String method, String path) throws Exception {
        assertThat(status(HttpMethod.valueOf(method), path, true)).isEqualTo(403);
    }

    private int status(HttpMethod method, String path, boolean signedIn) throws Exception {
        var call = request(method, path).contentType(MediaType.APPLICATION_JSON).content("{}");
        return mvc.perform(signedIn ? call.with(customer("matrix@visit.test")) : call).andReturn().getResponse().getStatus();
    }
}
