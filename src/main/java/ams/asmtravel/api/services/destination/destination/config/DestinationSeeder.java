package ams.asmtravel.api.services.destination.destination.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.DestinationThemes;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.destination.repository.DestinationRepository;
import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.region.domain.RegionKind;
import ams.asmtravel.api.services.location.region.dto.response.RegionResponse;
import ams.asmtravel.api.services.location.region.service.RegionService;
import ams.asmtravel.api.services.taxonomy.tag.domain.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Seeds the published starter destinations. Tourism regions come from each place's categories. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DestinationSeeder implements CatalogSeedContributor {

    private final DestinationRepository destinationRepository;
    private final RegionService regionService;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return PLACES;
    }

    @Override
    public boolean alreadySeeded() {
        return destinationRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> rows = SeedJson.rows(objectMapper, DestinationSeeder.class, "destinations.json");
        Instant now = Instant.now();
        List<RegionResponse> themes = regionService.getAll(RegionKind.TOURISM);
        for (JsonNode node : rows) {
            Destination destination = new Destination();
            destination.setSlug(SeedJson.text(node, "slug"));
            destination.setName(SeedJson.text(node, "name"));
            destination.setShortDescription(SeedJson.text(node, "shortDescription"));
            destination.setDescription(SeedJson.text(node, "description"));
            destination.setSlogan(SeedJson.text(node, "slogan"));
            destination.setSloganBy(SeedJson.text(node, "sloganBy"));
            /* As many views as the browse seed records: six visitors per point of popularity. */
            Integer popularity = SeedJson.integer(node, "popularity");
            destination.setViewCount(popularity == null ? 0L : popularity * 6L);
            destination.setAddress(SeedJson.text(node, "address"));
            destination.setLatitude(decimal(node, "latitude"));
            destination.setLongitude(decimal(node, "longitude"));
            destination.setCoverKey(context.imageKey(node));
            destination.setProvince(context.require(Province.class, SeedJson.text(node, "provinceSlug"), "province"));
            destination.setCategories(new HashSet<>(context.resolve(Category.class, SeedJson.strings(node, "categorySlugs"))));
            destination.setTags(new HashSet<>(context.resolve(Tag.class, SeedJson.strings(node, "tagSlugs"))));
            destination.setInterests(new HashSet<>(context.resolve(Interest.class, SeedJson.strings(node, "interestSlugs"))));
            destination.setTourismRegions(new HashSet<>(regionService.getTourismEntities(themesOf(destination, themes))));
            destination.setTheme(mainTheme(destination, themes));
            destination.setStatus(ListingStatus.PUBLISHED);
            destination.setPublishedAt(now);
            context.publish(Destination.class, destination.getSlug(), destinationRepository.save(destination));
        }
        log.info("Seeded {} destinations", rows.size());
    }

    /** Theme = the region most of its categories belong to; ties go A to Z. */
    private static String mainTheme(Destination destination, List<RegionResponse> themes) {
        Set<UUID> categories = destination.getCategories().stream().map(Category::getId).collect(Collectors.toSet());
        return themes.stream()
                .filter(theme -> DestinationThemes.isTheme(theme.code()))
                .filter(theme -> theme.categoryIds().stream().anyMatch(categories::contains))
                .sorted(java.util.Comparator
                        .comparingLong((RegionResponse theme) -> -theme.categoryIds().stream().filter(categories::contains).count())
                        .thenComparing(RegionResponse::name))
                .map(RegionResponse::code)
                .findFirst()
                .orElse(null);
    }

    /** The tourism regions that shelve one of the place's categories. */
    private static Set<UUID> themesOf(Destination destination, List<RegionResponse> themes) {
        Set<UUID> categories = destination.getCategories().stream().map(Category::getId).collect(Collectors.toSet());
        return themes.stream()
                .filter(theme -> theme.categoryIds().stream().anyMatch(categories::contains))
                .map(RegionResponse::id)
                .collect(Collectors.toSet());
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).decimalValue() : null;
    }

    @Override
    public void wipe() {
        destinationRepository.deleteAll();
        destinationRepository.flush();
    }
}
