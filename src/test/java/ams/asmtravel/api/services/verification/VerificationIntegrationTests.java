package ams.asmtravel.api.services.verification;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.support.TestCatalogue;
import com.jayway.jsonpath.JsonPath;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static ams.asmtravel.api.support.Callers.contributor;
import static ams.asmtravel.api.support.Callers.customer;
import static ams.asmtravel.api.support.Callers.editor;
import static ams.asmtravel.api.support.Callers.owner;
import static ams.asmtravel.api.support.Callers.staff;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifying is a BDO's job and an administrator's: they read the paperwork for
 * any place, whoever wrote it. An editor reviews and publishes, but verifies
 * nothing. Whether a place is verified is public; who decided and on what
 * evidence are the verifying desks' only.
 */
@SpringBootTest(properties = {"app.security.oauth2.enabled=true",
        // Two five-star reviews earn the travellers' seal here; the boss's real number is 10,000.
        "app.verification.traveler-seal.min-ratings=2", "app.verification.traveler-seal.min-average=4.5"})
@AutoConfigureMockMvc
@Transactional
class VerificationIntegrationTests {

    @MockitoBean MinioClient minioClient;
    @Autowired MockMvc mvc;
    @Autowired TestCatalogue catalogue;

    private Province province;
    private Destination contributorsPlace;
    private Destination staffPlace;

    @BeforeEach
    void places() throws Exception {
        when(minioClient.bucketExists(any())).thenReturn(true);
        province = catalogue.province();
        RequestPostProcessor writer = contributor("writer@verify.test");
        UUID writerId = UUID.fromString(JsonPath.read(mvc.perform(get("/api/v1/users/me").with(writer))
                .andReturn().getResponse().getContentAsString(), "$.id"));
        contributorsPlace = catalogue.place(province, "Kbal Spean", 13.6772, 104.0187, ListingStatus.PUBLISHED, writerId, Set.of());
        staffPlace = catalogue.published(province, "Phnom Bakheng", 13.4240, 103.8560);
    }

    @Test
    void aBdoVerifiesAnyPlaceAndAnEditorNone() throws Exception {
        RequestPostProcessor bdo = staff("bdo@verify.test", "BDO", "PERM_VERIFIED_BADGE_SET",
                "PERM_PARTNER_PUBLISH_APPROVE", "PERM_DESTINATION_EDIT_ANY");
        // Every place is on the queue, whoever wrote it: a contributor's and the staff's own.
        mvc.perform(get("/api/v1/destination-verifications/queue").with(bdo))
                .andExpect(jsonPath("$[*].destinationId", hasItem(contributorsPlace.getId().toString())))
                .andExpect(jsonPath("$[*].destinationId", hasItem(staffPlace.getId().toString())));

        String evidence = "verification/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".jpg";
        mvc.perform(post("/api/v1/destination-verifications").with(bdo).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + contributorsPlace.getId() + "\",\"verified\":true,\"basis\":\"PAID\","
                                + "\"reference\":\"INV-2026-001\",\"evidenceKey\":\"" + evidence + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.decidedByName").value("Staff bdo"))
                .andExpect(jsonPath("$.evidenceUrl").value("http://localhost:8080/api/v1/media/files/" + evidence));
        mvc.perform(post("/api/v1/destination-verifications").with(bdo).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + staffPlace.getId() + "\",\"verified\":true,\"basis\":\"PAID\",\"reference\":\"INV-2026-002\"}"))
                .andExpect(status().isCreated());

        // An editor reviews and publishes; the paperwork is not theirs to read.
        mvc.perform(get("/api/v1/destination-verifications/queue").with(editor())).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/destination-verifications").with(editor()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + contributorsPlace.getId() + "\",\"verified\":true}"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/destination-verifications/status")
                        .param("destinationIds", contributorsPlace.getId().toString(), staffPlace.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].verified").value(true))
                .andExpect(jsonPath("$[1].verified").value(true));
        mvc.perform(get("/api/v1/destination-verifications/status"))
                .andExpect(jsonPath("$[*].destinationId", hasItem(contributorsPlace.getId().toString())));
    }

    @Test
    void revokingNeedsAReasonAndSomethingToRevoke() throws Exception {
        String place = "\"destinationId\":\"" + staffPlace.getId() + "\"";
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + place + ",\"verified\":false,\"note\":\"Closed\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + place + ",\"verified\":true,\"basis\":\"PAID\",\"reference\":\"INV-2026-014\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + place + ",\"verified\":false}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + place + ",\"verified\":false,\"note\":\"The invoice lapsed\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/destination-verifications").param("destinationId", staffPlace.getId().toString()).with(owner()))
                .andExpect(jsonPath("$[0].verified").value(false))
                .andExpect(jsonPath("$[0].note").value("The invoice lapsed"))
                .andExpect(jsonPath("$[1].verified").value(true));
        mvc.perform(get("/api/v1/destination-verifications/status").param("destinationIds", staffPlace.getId().toString()))
                .andExpect(jsonPath("$[0].verified").value(false));
    }

    /** Verified on nothing is not verified: a paid verification needs the invoice, or at least its number. */
    @Test
    void verifyingNeedsTheInvoiceOrItsNumber() throws Exception {
        String place = "\"destinationId\":\"" + staffPlace.getId() + "\"";
        for (String nothing : new String[] {"", ",\"reference\":\"  \",\"evidenceKey\":\"\"", ",\"note\":\"Looks fine\""}) {
            mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                            .content("{" + place + ",\"verified\":true,\"basis\":\"PAID\"" + nothing + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Attach the invoice or receipt, or give its number, to verify"));
        }
        String evidence = "verification/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".pdf";
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + place + ",\"verified\":true,\"basis\":\"PAID\",\"evidenceKey\":\"" + evidence + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.basis").value("PAID"))
                .andExpect(jsonPath("$.evidenceKey").value(evidence));
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + place + ",\"verified\":true,\"basis\":\"PAID\",\"reference\":\"RCPT-88\"}"))
                .andExpect(status().isCreated());
    }

    /** Evidence has to be a file this API holds, in the private folder. */
    @Test
    void evidenceComesFromTheVerificationFolder() throws Exception {
        String elsewhere = "destination/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".jpg";
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + staffPlace.getId() + "\",\"verified\":true,\"basis\":\"PAID\",\"evidenceKey\":\"" + elsewhere + "\"}"))
                .andExpect(status().isBadRequest());

        String file = "/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".jpg";
        mvc.perform(get("/api/v1/media/files/verification" + file)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/media/files/verification" + file).with(customer("nosy@visit.test"))).andExpect(status().isForbidden());

        // The folder is matched as it is stored; another spelling reaches no file, public rule or not.
        for (String spelling : new String[] {"VERIFICATION", "Verification", "verificatioN"}) {
            mvc.perform(get("/api/v1/media/files/" + spelling + file)).andExpect(status().isNotFound());
            mvc.perform(get("/api/v1/media/files/" + spelling + file).with(customer("nosy@visit.test"))).andExpect(status().isNotFound());
        }
    }

    @Test
    void theRecordIsTheDesksOnly() throws Exception {
        String history = "/api/v1/destination-verifications?destinationId=" + staffPlace.getId();
        mvc.perform(get(history)).andExpect(status().isUnauthorized());
        mvc.perform(get(history).with(customer("curious@visit.test"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/destination-verifications/queue").with(contributor("writer@verify.test"))).andExpect(status().isForbidden());
        mvc.perform(get(history).with(editor())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/destination-verifications/queue").with(owner()))
                .andExpect(jsonPath("$[*].destinationId", hasItem(staffPlace.getId().toString())));
    }

    /** An official site is verified on its standing: no invoice, no agreement, a note at most. */
    @Test
    void anOfficialSiteNeedsNoPaperwork() throws Exception {
        String place = "\"destinationId\":\"" + staffPlace.getId() + "\"";
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + place + ",\"verified\":true,\"reference\":\"INV-2026-020\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Say what the verification rests on: an official site, a partnership, or a payment"));
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + place + ",\"verified\":true,\"basis\":\"OFFICIAL_SITE\",\"note\":\"On the Ministry of Tourism's list\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.basis").value("OFFICIAL_SITE"));
        mvc.perform(get("/api/v1/destination-verifications/status").param("destinationIds", staffPlace.getId().toString()))
                .andExpect(jsonPath("$[0].verified").value(true))
                .andExpect(jsonPath("$[0].basis").value("OFFICIAL_SITE"))
                .andExpect(jsonPath("$[0].travelerRecommended").value(false));
        // The travellers' seal is theirs to earn, not a desk's to file.
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + place + ",\"verified\":true,\"basis\":\"TRAVELER_SCORE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Recommended by Traveler is earned from reviews; it cannot be filed"));
    }

    /** A partner's verification rests on the agreement, and only a partner's own listing can carry it. */
    @Test
    void aPartnersVerificationIsForAPartnersOwnListing() throws Exception {
        RequestPostProcessor host = staff("host@verify.test", "PARTNER", "PERM_LISTING_EDIT_OWN");
        UUID hostId = UUID.fromString(JsonPath.read(mvc.perform(get("/api/v1/users/me").with(host))
                .andReturn().getResponse().getContentAsString(), "$.id"));
        Destination partnersPlace = catalogue.place(province, "Sala Lodges", 13.3622, 103.8597, ListingStatus.PUBLISHED, hostId, Set.of());
        String ours = "\"destinationId\":\"" + staffPlace.getId() + "\"";
        String theirs = "\"destinationId\":\"" + partnersPlace.getId() + "\"";

        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + ours + ",\"verified\":true,\"basis\":\"PARTNER\",\"reference\":\"AGR-2026-07\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Only a partner's own listing is verified as a partner's; this one was written by our team"));
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + theirs + ",\"verified\":true,\"basis\":\"PARTNER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Attach the partnership agreement, or give its reference, to verify"));
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{" + theirs + ",\"verified\":true,\"basis\":\"PARTNER\",\"reference\":\"AGR-2026-07\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.basis").value("PARTNER"));
        mvc.perform(get("/api/v1/destination-verifications/queue").with(owner()))
                .andExpect(jsonPath("$[?(@.destinationId=='" + partnersPlace.getId() + "')].basis").value("PARTNER"))
                .andExpect(jsonPath("$[?(@.destinationId=='" + partnersPlace.getId() + "')].verified").value(true));
    }

    /** Enough reviews at a high enough average verify a place on their own: Recommended by Traveler. */
    @Test
    void theTravellersVerifyAPlaceWithTheirReviews() throws Exception {
        String status = "/api/v1/destination-verifications/status";
        String id = staffPlace.getId().toString();
        mvc.perform(get(status).param("destinationIds", id))
                .andExpect(jsonPath("$[0].verified").value(false))
                .andExpect(jsonPath("$[0].travelerRecommended").value(false))
                .andExpect(jsonPath("$[0].ratingCount").value(0));
        for (String visitor : new String[] {"a@visit.test", "b@visit.test"}) {
            mvc.perform(post("/api/v1/reviews").with(customer(visitor)).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"targetType\":\"DESTINATION\",\"targetId\":\"" + id + "\",\"rating\":5,\"content\":\"Worth the climb\"}"))
                    .andExpect(status().isCreated());
        }
        mvc.perform(get(status).param("destinationIds", id))
                .andExpect(jsonPath("$[0].verified").value(true))
                .andExpect(jsonPath("$[0].basis").value("TRAVELER_SCORE"))
                .andExpect(jsonPath("$[0].travelerRecommended").value(true))
                .andExpect(jsonPath("$[0].ratingCount").value(2));
        // Among the verified places, and on the queue as the travellers' doing; nothing for a desk to revoke.
        mvc.perform(get(status)).andExpect(jsonPath("$[*].destinationId", hasItem(id)));
        mvc.perform(get("/api/v1/destination-verifications/queue").with(owner()))
                .andExpect(jsonPath("$[?(@.destinationId=='" + id + "')].basis").value("TRAVELER_SCORE"))
                .andExpect(jsonPath("$[?(@.destinationId=='" + id + "')].travelerRecommended").value(true));
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + id + "\",\"verified\":false,\"note\":\"Not ours to take away\"}"))
                .andExpect(status().isConflict());
        // A paid verification on top: the badge rests on the invoice, and the seal stays the travellers'.
        mvc.perform(post("/api/v1/destination-verifications").with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"destinationId\":\"" + id + "\",\"verified\":true,\"basis\":\"PAID\",\"reference\":\"INV-2026-050\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get(status).param("destinationIds", id))
                .andExpect(jsonPath("$[0].basis").value("PAID"))
                .andExpect(jsonPath("$[0].travelerRecommended").value(true));
    }
}
