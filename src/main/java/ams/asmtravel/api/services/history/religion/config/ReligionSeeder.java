package ams.asmtravel.api.services.history.religion.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.history.religion.domain.Religion;
import ams.asmtravel.api.services.history.religion.repository.ReligionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Hinduism, Buddhism and animism, published under their slugs for the timelines. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReligionSeeder implements CatalogSeedContributor {

    private final ReligionRepository religionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return TERMS;
    }

    @Override
    public boolean alreadySeeded() {
        return religionRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> religions = SeedJson.rows(objectMapper, ReligionSeeder.class, "religions.json");
        for (JsonNode node : religions) {
            Religion religion = new Religion();
            religion.setName(SeedJson.text(node, "name"));
            religion.setDescription(SeedJson.text(node, "description"));
            religion.setIconKey(SeedJson.text(node, "iconKey"));
            context.publish(Religion.class, SeedJson.text(node, "slug"), religionRepository.save(religion));
        }
        log.info("Seeded {} religions", religions.size());
    }

    @Override
    public void wipe() {
        religionRepository.deleteAll();
        religionRepository.flush();
    }
}
