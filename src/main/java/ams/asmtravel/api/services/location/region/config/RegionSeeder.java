package ams.asmtravel.api.services.location.region.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.location.region.domain.Region;
import ams.asmtravel.api.services.location.region.domain.RegionKind;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import ams.asmtravel.api.services.taxonomy.tag.domain.Tag;
import ams.asmtravel.api.services.location.region.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.List;

/** Seeds the Discover regions: 5 tourism zones (for provinces) and 9 tourism regions with their categories and tags. */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegionSeeder implements CatalogSeedContributor {

    private final RegionRepository regionRepository;
    private final ObjectMapper objectMapper;

    /** After the vocabularies it links to, before the provinces that name it. */
    @Override
    public int getOrder() {
        return REGIONS;
    }

    @Override
    public boolean alreadySeeded() {
        return regionRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> regions = SeedJson.rows(objectMapper, RegionSeeder.class, "regions.json");
        /* Numbered within each kind in file order (the curators' order). */
        java.util.Map<RegionKind, Integer> numbered = new java.util.EnumMap<>(RegionKind.class);
        for (JsonNode node : regions) {
            Region region = new Region();
            region.setCode(SeedJson.text(node, "code"));
            region.setName(SeedJson.text(node, "name"));
            region.setDescription(SeedJson.text(node, "description"));
            String kind = SeedJson.text(node, "kind");
            region.setKind(kind == null ? RegionKind.ZONE : RegionKind.valueOf(kind));
            region.setSortOrder(numbered.merge(region.getKind(), 1, Integer::sum) - 1);
            /* Categories and filters in file order. */
            region.setCategories(new java.util.ArrayList<>(context.resolve(Category.class, SeedJson.strings(node, "categories"))));
            region.setTags(new java.util.ArrayList<>(context.resolve(Tag.class, SeedJson.strings(node, "tags"))));
            region.setInterests(new HashSet<>(context.resolve(Interest.class, SeedJson.strings(node, "interests"))));
            context.publish(Region.class, region.getCode(), regionRepository.save(region));
        }
        log.info("Seeded {} regions", regions.size());
    }
}
