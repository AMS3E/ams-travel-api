package ams.asmtravel.api.services.location.province.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.region.domain.Region;
import ams.asmtravel.api.services.location.province.repository.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** Cambodia's 25 provinces, each in its zone, keyed by slug ({@code siem-reap}) for the destination seed. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProvinceSeeder implements CatalogSeedContributor {

    private final ProvinceRepository provinceRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return GROUPINGS;
    }

    @Override
    public boolean alreadySeeded() {
        return provinceRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> provinces = SeedJson.rows(objectMapper, ProvinceSeeder.class, "provinces.json");
        for (JsonNode node : provinces) {
            Province province = new Province();
            province.setSlug(SeedJson.text(node, "slug"));
            province.setName(SeedJson.text(node, "name"));
            province.setRegion(context.require(Region.class, SeedJson.text(node, "regionCode"), "region"));
            if (node.hasNonNull("latitude") && node.hasNonNull("longitude")) {
                province.setLatitude(node.get("latitude").decimalValue());
                province.setLongitude(node.get("longitude").decimalValue());
            }
            context.publish(Province.class, SeedJson.text(node, "slug"), provinceRepository.save(province));
        }
        log.info("Seeded {} provinces", provinces.size());
    }
}
