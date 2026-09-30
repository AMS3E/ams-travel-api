package ams.asmtravel.api.services.history.architecture.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.history.architecture.domain.ArchitectureStyle;
import ams.asmtravel.api.services.history.period.domain.HistoricalPeriod;
import ams.asmtravel.api.services.history.architecture.repository.ArchitectureStyleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Twelve styles from Sambor Prei Kuk to the colonial revival, each in its period. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ArchitectureStyleSeeder implements CatalogSeedContributor {

    private final ArchitectureStyleRepository styleRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return GROUPINGS;
    }

    @Override
    public boolean alreadySeeded() {
        return styleRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> styles = SeedJson.rows(objectMapper, ArchitectureStyleSeeder.class, "architecture-styles.json");
        for (JsonNode node : styles) {
            ArchitectureStyle style = new ArchitectureStyle();
            style.setName(SeedJson.text(node, "name"));
            style.setDescription(SeedJson.text(node, "description"));
            String periodSlug = SeedJson.text(node, "periodSlug");
            HistoricalPeriod period = periodSlug == null ? null : context.find(HistoricalPeriod.class, periodSlug);
            style.setPeriodId(period == null ? null : period.getId());
            context.publish(ArchitectureStyle.class, SeedJson.text(node, "slug"), styleRepository.save(style));
        }
        log.info("Seeded {} architecture styles", styles.size());
    }
}
