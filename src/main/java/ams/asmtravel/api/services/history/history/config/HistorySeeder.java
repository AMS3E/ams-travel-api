package ams.asmtravel.api.services.history.history.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.history.history.domain.DestinationHistory;
import ams.asmtravel.api.services.history.period.domain.HistoricalPeriod;
import ams.asmtravel.api.services.history.history.repository.DestinationHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** The timelines of fifteen places, from Sambor Prei Kuk to the National Museum: an entry each. */
@Component
@RequiredArgsConstructor
@Slf4j
public class HistorySeeder implements CatalogSeedContributor {

    private final DestinationHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return DETAILS;
    }

    @Override
    public boolean alreadySeeded() {
        return historyRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> histories = SeedJson.rows(objectMapper, HistorySeeder.class, "destination-histories.json");
        int seeded = 0;
        for (JsonNode node : histories) {
            String slug = SeedJson.text(node, "destinationSlug");
            Destination destination = context.find(Destination.class, slug);
            if (destination == null) {
                log.warn("History seed names unknown destination {}", slug);
                continue;
            }
            DestinationHistory moment = new DestinationHistory();
            moment.setDestination(destination);
            String periodSlug = SeedJson.text(node, "periodSlug");
            HistoricalPeriod period = periodSlug == null ? null : context.find(HistoricalPeriod.class, periodSlug);
            moment.setHistoricalPeriodId(period == null ? null : period.getId());
            moment.setTitle(SeedJson.text(node, "title"));
            moment.setDescription(SeedJson.text(node, "description"));
            moment.setEventYear(SeedJson.integer(node, "eventYear"));
            moment.setSourceReference(SeedJson.text(node, "sourceReference"));
            historyRepository.save(moment);
            seeded++;
        }
        log.info("Seeded the timelines of {} destinations", seeded);
    }
}
