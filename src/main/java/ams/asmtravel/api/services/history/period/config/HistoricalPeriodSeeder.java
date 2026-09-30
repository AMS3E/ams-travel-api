package ams.asmtravel.api.services.history.period.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.history.period.domain.HistoricalPeriod;
import ams.asmtravel.api.services.history.period.repository.HistoricalPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Pre-Angkor, Angkor and Post-Angkor, published under their slugs for styles and timelines. */
@Component
@RequiredArgsConstructor
@Slf4j
public class HistoricalPeriodSeeder implements CatalogSeedContributor {

    private final HistoricalPeriodRepository periodRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return TERMS;
    }

    @Override
    public boolean alreadySeeded() {
        return periodRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> periods = SeedJson.rows(objectMapper, HistoricalPeriodSeeder.class, "historical-periods.json");
        for (JsonNode node : periods) {
            HistoricalPeriod period = new HistoricalPeriod();
            period.setName(SeedJson.text(node, "name"));
            period.setStartYear(SeedJson.integer(node, "startYear"));
            period.setEndYear(SeedJson.integer(node, "endYear"));
            period.setDescription(SeedJson.text(node, "description"));
            context.publish(HistoricalPeriod.class, SeedJson.text(node, "slug"), periodRepository.save(period));
        }
        log.info("Seeded {} historical periods", periods.size());
    }
}
