package ams.asmtravel.api.services.history.destinationmonarch.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.history.destinationmonarch.domain.DestinationMonarch;
import ams.asmtravel.api.services.history.monarch.domain.Monarch;
import ams.asmtravel.api.services.history.destinationmonarch.repository.DestinationMonarchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** The monarchs behind the seeded places: Suryavarman II at Angkor Wat, Jayavarman VII at the Bayon. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DestinationMonarchSeeder implements CatalogSeedContributor {

    private final DestinationMonarchRepository linkRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return DETAILS;
    }

    @Override
    public boolean alreadySeeded() {
        return linkRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        int written = 0;
        for (JsonNode node : SeedJson.rows(objectMapper, DestinationMonarchSeeder.class, "destination-monarchs.json")) {
            String slug = SeedJson.text(node, "destinationSlug");
            Destination destination = context.find(Destination.class, slug);
            if (destination == null) {
                log.warn("Monarch seed names unknown destination {}", slug);
                continue;
            }
            for (Monarch monarch : context.resolve(Monarch.class, SeedJson.strings(node, "monarchSlugs"))) {
                linkRepository.save(new DestinationMonarch(destination, monarch.getId(), null));
                written++;
            }
        }
        log.info("Seeded {} ties between destinations and monarchs", written);
    }

    @Override
    public void wipe() {
        linkRepository.deleteAll();
        linkRepository.flush();
    }
}
