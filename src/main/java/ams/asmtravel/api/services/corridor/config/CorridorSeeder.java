package ams.asmtravel.api.services.corridor.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.corridor.domain.CorridorStatus;
import ams.asmtravel.api.services.corridor.repository.CorridorRepository;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

/** Seeds the published corridors; days and stops are seeded later, after destinations. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CorridorSeeder implements CatalogSeedContributor {

    private final CorridorRepository corridorRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return GROUPINGS;
    }

    @Override
    public boolean alreadySeeded() {
        return corridorRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> corridors = SeedJson.rows(objectMapper, CorridorSeeder.class, "corridors.json");
        Instant now = Instant.now();
        int place = 0;
        for (JsonNode node : corridors) {
            Corridor corridor = new Corridor();
            corridor.setSlug(SeedJson.text(node, "slug"));
            corridor.setName(SeedJson.text(node, "name"));
            corridor.setShortDescription(SeedJson.text(node, "shortDescription"));
            corridor.setDescription(SeedJson.text(node, "description"));
            corridor.setCoverKey(context.imageKey(node));
            corridor.setEstimatedBudgetMin(decimal(node, "estimatedBudgetMin"));
            corridor.setEstimatedBudgetMax(decimal(node, "estimatedBudgetMax"));
            /* Keeps the curated file order. */
            corridor.setSortOrder(place++);
            corridor.setInterests(new HashSet<>(context.resolve(Interest.class, SeedJson.strings(node, "interests"))));
            corridor.setStatus(CorridorStatus.PUBLISHED);
            corridor.setPublishedAt(now);
            context.publish(Corridor.class, corridor.getSlug(), corridorRepository.save(corridor));
        }
        log.info("Seeded {} corridors", corridors.size());
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).decimalValue() : null;
    }

    @Override
    public void wipe() {
        corridorRepository.deleteAll();
        corridorRepository.flush();
    }
}
