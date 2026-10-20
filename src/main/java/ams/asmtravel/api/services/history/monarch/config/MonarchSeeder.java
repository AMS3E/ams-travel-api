package ams.asmtravel.api.services.history.monarch.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.history.monarch.domain.Monarch;
import ams.asmtravel.api.services.history.monarch.repository.MonarchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Fifteen kings from Isanavarman I to Sisowath, published under their slugs for the timelines. */
@Component
@RequiredArgsConstructor
@Slf4j
public class MonarchSeeder implements CatalogSeedContributor {

    private final MonarchRepository monarchRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return TERMS;
    }

    @Override
    public boolean alreadySeeded() {
        return monarchRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> monarchs = SeedJson.rows(objectMapper, MonarchSeeder.class, "monarchs.json");
        for (JsonNode node : monarchs) {
            Monarch monarch = new Monarch();
            monarch.setName(SeedJson.text(node, "name"));
            monarch.setTitle(SeedJson.text(node, "title"));
            monarch.setReignStartYear(SeedJson.integer(node, "reignStartYear"));
            monarch.setReignEndYear(SeedJson.integer(node, "reignEndYear"));
            monarch.setDescription(SeedJson.text(node, "description"));
            context.publish(Monarch.class, SeedJson.text(node, "slug"), monarchRepository.save(monarch));
        }
        log.info("Seeded {} monarchs", monarchs.size());
    }

    @Override
    public void wipe() {
        monarchRepository.deleteAll();
        monarchRepository.flush();
    }
}
