package ams.asmtravel.api.services.taxonomy.interest.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import ams.asmtravel.api.services.taxonomy.interest.repository.InterestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/** The interests the catalogue ships with. */
@Component
@RequiredArgsConstructor
@Slf4j
public class InterestSeeder implements CatalogSeedContributor {

    private final InterestRepository interestRepository;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return TERMS;
    }

    @Override
    public boolean alreadySeeded() {
        return interestRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> rows = SeedJson.rows(objectMapper, InterestSeeder.class, "interests.json");
        /* Numbered within their group in the file's order, which is the curators'. */
        java.util.Map<String, Integer> numbered = new java.util.HashMap<>();
        for (JsonNode node : rows) {
            Interest interest = new Interest();
            interest.setSlug(SeedJson.text(node, "slug"));
            interest.setName(SeedJson.text(node, "name"));
            interest.setDescription(SeedJson.text(node, "description"));
            interest.setGroupName(SeedJson.text(node, "group"));
            interest.setIconKey(SeedJson.text(node, "icon"));
            /* Its picture on the Explore shelf: one of the catalogue's own photographs, named by file. */
            interest.setCoverKey(context.imageKey(node));
            interest.setSortOrder(numbered.merge(String.valueOf(interest.getGroupName()), 1, Integer::sum) - 1);
            context.publish(Interest.class, interest.getSlug(), interestRepository.save(interest));
        }
        log.info("Seeded {} interests", rows.size());
    }

    @Override
    public void wipe() {
        interestRepository.deleteAll();
        interestRepository.flush();
    }
}
