package ams.asmtravel.api.services.taxonomy.category.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.taxonomy.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** The categories the catalogue ships with. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CategorySeeder implements CatalogSeedContributor {

    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return TERMS;
    }

    @Override
    public boolean alreadySeeded() {
        return categoryRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> rows = SeedJson.rows(objectMapper, CategorySeeder.class, "categories.json");
        for (JsonNode node : rows) {
            Category category = new Category();
            category.setSlug(SeedJson.text(node, "slug"));
            category.setName(SeedJson.text(node, "name"));
            Integer sortOrder = SeedJson.integer(node, "sortOrder");
            category.setSortOrder(sortOrder == null ? 0 : sortOrder);
            context.publish(Category.class, category.getSlug(), categoryRepository.save(category));
        }
        log.info("Seeded {} categories", rows.size());
    }
}
