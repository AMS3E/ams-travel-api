package ams.asmtravel.api.services.history.regioncoverage.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.history.period.domain.HistoricalPeriod;
import ams.asmtravel.api.services.history.regioncoverage.domain.RegionCoverage;
import ams.asmtravel.api.services.history.regioncoverage.repository.RegionCoverageRepository;
import ams.asmtravel.api.services.location.region.domain.Region;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Seeds each region's succession, e.g. the eight royal capitals of Ancient Capitals, in order. */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegionCoverageSeeder implements CatalogSeedContributor {

    private final RegionCoverageRepository coverageRepository;
    private final ObjectMapper objectMapper;

    /** After the regions it belongs to and the places it points at. */
    @Override
    public int getOrder() {
        return DETAILS;
    }

    @Override
    public boolean alreadySeeded() {
        return coverageRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> regions = SeedJson.rows(objectMapper, RegionCoverageSeeder.class, "region-coverage.json");
        int written = 0;
        for (JsonNode node : regions) {
            String code = SeedJson.text(node, "region");
            Region region = context.require(Region.class, code, "region");
            /* The intro line belongs to the region; its row is still managed here, so setting the field is enough. */
            region.setHistoryIntro(SeedJson.text(node, "intro"));

            int order = 0;
            for (JsonNode entry : node.get("entries")) {
                RegionCoverage coverage = new RegionCoverage();
                coverage.setRegion(region);
                coverage.setSortOrder(order++);
                coverage.setTitle(SeedJson.text(entry, "title"));
                coverage.setTitleKm(SeedJson.text(entry, "titleKm"));
                coverage.setDateRange(SeedJson.text(entry, "dateRange"));
                coverage.setDescription(SeedJson.text(entry, "description"));

                HistoricalPeriod period = context.find(HistoricalPeriod.class, SeedJson.text(entry, "period"));
                coverage.setHistoricalPeriodId(period == null ? null : period.getId());
                /* A capital with no place in the catalogue simply has nothing to open. */
                coverage.setDestination(context.find(Destination.class, SeedJson.text(entry, "destination")));

                coverageRepository.save(coverage);
                written++;
            }
        }
        log.info("Seeded {} historical coverage entries across {} regions", written, regions.size());
    }
}
