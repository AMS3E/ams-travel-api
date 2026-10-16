package ams.asmtravel.api.services.taxonomy.tag.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.taxonomy.tag.domain.Tag;
import ams.asmtravel.api.services.taxonomy.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** The tags the catalogue ships with. */
@Component
@RequiredArgsConstructor
@Slf4j
public class TagSeeder implements CatalogSeedContributor {

    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return TERMS;
    }

    @Override
    public boolean alreadySeeded() {
        return tagRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> rows = SeedJson.rows(objectMapper, TagSeeder.class, "tags.json");
        for (JsonNode node : rows) {
            Tag tag = new Tag();
            tag.setSlug(SeedJson.text(node, "slug"));
            tag.setName(SeedJson.text(node, "name"));
            context.publish(Tag.class, tag.getSlug(), tagRepository.save(tag));
        }
        log.info("Seeded {} tags", rows.size());
    }

    @Override
    public void wipe() {
        tagRepository.deleteAll();
        tagRepository.flush();
    }
}
