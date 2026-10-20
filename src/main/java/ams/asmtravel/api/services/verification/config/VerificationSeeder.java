package ams.asmtravel.api.services.verification.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.user.user.domain.User;
import ams.asmtravel.api.services.publishing.domain.PublishingTargetType;
import ams.asmtravel.api.services.verification.domain.DestinationVerification;
import ams.asmtravel.api.services.verification.domain.VerificationBasis;
import ams.asmtravel.api.services.verification.repository.DestinationVerificationRepository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/** Seeds the Verified decisions on the official sites, filed under the seed's editorial desk. */
@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationSeeder implements CatalogSeedContributor {

    /** The seed's editorial desk, as the travellers' seed publishes it. */
    private static final String DESK_KEY = "ams-desk";

    private final DestinationVerificationRepository verificationRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return ENGAGEMENT;
    }

    @Override
    public boolean alreadySeeded() {
        return verificationRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> rows = SeedJson.rows(objectMapper, VerificationSeeder.class, "verifications.json");
        User desk = context.require(User.class, DESK_KEY, "editorial desk");
        int written = 0;
        for (JsonNode node : rows) {
            Destination place = context.find(Destination.class, SeedJson.text(node, "destinationSlug"));
            if (place == null) {
                log.warn("Verification of unknown destination {}", SeedJson.text(node, "destinationSlug"));
                continue;
            }
            DestinationVerification decision = new DestinationVerification();
            decision.setTargetType(PublishingTargetType.DESTINATION);
            decision.setTargetId(place.getId());
            decision.setVerified(true);
            VerificationBasis basis = SeedJson.enumValue(node, "basis", VerificationBasis.class);
            decision.setBasis(basis == null ? VerificationBasis.OFFICIAL_SITE : basis);
            decision.setReference(SeedJson.text(node, "reference"));
            decision.setNote(SeedJson.text(node, "note"));
            decision.setDecidedBy(desk.getId());
            verificationRepository.save(decision);
            written += 1;
        }
        log.info("Seeded {} verification decisions", written);
    }

    @Override
    public void wipe() {
        verificationRepository.deleteAllInBatch();
    }
}
