package ams.asmtravel.api;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.corridor.config.CorridorSeeder;
import ams.asmtravel.api.services.corridor.config.CorridorItinerarySeeder;
import ams.asmtravel.api.services.destination.destination.config.DestinationSeeder;
import ams.asmtravel.api.services.location.province.config.ProvinceSeeder;
import ams.asmtravel.api.services.location.region.config.RegionSeeder;
import ams.asmtravel.api.services.media.config.MinioConfig;
import ams.asmtravel.api.services.taxonomy.category.config.CategorySeeder;
import ams.asmtravel.api.services.taxonomy.interest.config.InterestSeeder;
import ams.asmtravel.api.services.taxonomy.tag.config.TagSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The whole catalogue seed: every service's share, in the order startup runs
 * them. A row naming a slug nobody published is skipped with a log line
 * rather than failing startup, so a file that lost its way would load short
 * and say so only there. This counts what each file describes against what
 * arrived. Its own database: every other context starts empty.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:ams_seed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;"
        + "INIT=CREATE DOMAIN IF NOT EXISTS INET AS VARCHAR(45)")
class CatalogSeedTests {

    @Autowired List<CatalogSeedContributor> contributors;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void everyRowEachServiceSeedsArrives() throws IOException {
        CatalogSeedContext context = new CatalogSeedContext(seedImageKeys());
        transactions.executeWithoutResult(status -> contributors.forEach(contributor -> contributor.seed(context)));

        // Five tourism zones the provinces belong to, and the nine tourism regions the Discover layer browses by.
        assertThat(count("taxonomy.regions")).isEqualTo(14).isEqualTo(rows(RegionSeeder.class, "regions.json").size());
        assertThat(count("taxonomy.regions where kind = 'ZONE'")).isEqualTo(5);
        assertThat(count("taxonomy.regions where kind = 'TOURISM'")).isEqualTo(9);
        assertThat(count("taxonomy.region_categories"))
                .isEqualTo(nested(RegionSeeder.class, "regions.json", "categories"));
        assertThat(count("taxonomy.region_tags")).isEqualTo(nested(RegionSeeder.class, "regions.json", "tags"));
        // Ancient Capitals' search filters are six plain tags: Century, King, Architecture Style, UNESCO, Religion, Historical Period.
        assertThat(count("taxonomy.region_tags rt join taxonomy.regions r on r.id = rt.region_id "
                + "where r.code = 'ANCIENT-CAPITALS'")).isEqualTo(6);
        assertThat(count("destination.destination_tags dt join taxonomy.tags t on t.id = dt.tag_id where t.slug = 'king'"))
                .isPositive();
        // Each tourism region offers its own interests, and every interest of places belongs to one at least.
        assertThat(count("taxonomy.region_interests")).isEqualTo(nested(RegionSeeder.class, "regions.json", "interests"));
        assertThat(count("taxonomy.regions r where r.kind = 'TOURISM' and not exists "
                + "(select 1 from taxonomy.region_interests ri where ri.region_id = r.id)")).isZero();
        assertThat(count("taxonomy.interests i where i.group_name <> 'Tourism Corridors' and not exists "
                + "(select 1 from taxonomy.region_interests ri where ri.interest_id = i.id)")).isZero();
        // A Tourism Corridors interest is a corridor's instead: each corridor is under its own.
        assertThat(count("taxonomy.interests where group_name = 'Tourism Corridors'")).isEqualTo(5);
        assertThat(count("taxonomy.corridor_interests")).isEqualTo(nested(CorridorSeeder.class, "corridors.json", "interests")).isEqualTo(5);
        assertThat(count("taxonomy.interests i where i.group_name = 'Tourism Corridors' and not exists "
                + "(select 1 from taxonomy.corridor_interests ci where ci.interest_id = i.id)")).isZero();
        // Every interest the Discover layer lists is under one of its six groups.
        assertThat(count("taxonomy.interests where group_name is not null")).isEqualTo(33);
        // The real data: no interest outside the six groups, and five tourism corridors.
        assertThat(count("taxonomy.interests where group_name is null")).isZero();
        assertThat(count("taxonomy.corridors")).isEqualTo(5);
        // In the curators' order, which the seed files keep: the regions 1 to 9, each region's own
        // categories and filters, the interests within their group, and the corridors.
        assertThat(jdbc.queryForList("select code from taxonomy.regions where kind = 'TOURISM' order by sort_order", String.class))
                .isEqualTo(rows(RegionSeeder.class, "regions.json").stream()
                        .filter(row -> "TOURISM".equals(SeedJson.text(row, "kind"))).map(row -> SeedJson.text(row, "code")).toList())
                .startsWith("ANCIENT-CAPITALS", "NORTHEASTERN", "MEKONG-TONLE-SAP");
        for (JsonNode region : rows(RegionSeeder.class, "regions.json")) {
            String code = SeedJson.text(region, "code");
            assertThat(jdbc.queryForList("select c.slug from taxonomy.region_categories rc join taxonomy.regions r on r.id = rc.region_id "
                    + "join taxonomy.categories c on c.id = rc.category_id where r.code = ? order by rc.sort_order", String.class, code))
                    .as(code + "'s categories").isEqualTo(SeedJson.strings(region, "categories"));
            assertThat(jdbc.queryForList("select t.slug from taxonomy.region_tags rt join taxonomy.regions r on r.id = rt.region_id "
                    + "join taxonomy.tags t on t.id = rt.tag_id where r.code = ? order by rt.sort_order", String.class, code))
                    .as(code + "'s search filters").isEqualTo(SeedJson.strings(region, "tags"));
        }
        for (JsonNode interest : rows(InterestSeeder.class, "interests.json")) {
            String group = SeedJson.text(interest, "group");
            assertThat(jdbc.queryForList("select slug from taxonomy.interests where group_name = ? order by sort_order", String.class, group))
                    .as(group).isEqualTo(rows(InterestSeeder.class, "interests.json").stream()
                            .filter(row -> group.equals(SeedJson.text(row, "group"))).map(row -> SeedJson.text(row, "slug")).toList());
        }
        assertThat(jdbc.queryForList("select slug from taxonomy.corridors order by sort_order", String.class))
                .isEqualTo(rows(CorridorSeeder.class, "corridors.json").stream().map(row -> SeedJson.text(row, "slug")).toList());
        assertThat(count("taxonomy.provinces")).isEqualTo(25).isEqualTo(rows(ProvinceSeeder.class, "provinces.json").size());
        assertThat(count("taxonomy.categories")).isEqualTo(rows(CategorySeeder.class, "categories.json").size());
        assertThat(count("taxonomy.tags")).isEqualTo(rows(TagSeeder.class, "tags.json").size());
        assertThat(count("taxonomy.interests")).isEqualTo(rows(InterestSeeder.class, "interests.json").size());
        assertThat(count("taxonomy.corridors")).isEqualTo(rows(CorridorSeeder.class, "corridors.json").size());

        assertThat(count("destination.destinations")).isEqualTo(rows(DestinationSeeder.class, "destinations.json").size());
        assertThat(count("destination.destinations where cover_key is null")).isZero();
        // Every shipped place has its theme: the tourism region most of its categories come from.
        assertThat(count("destination.destinations where theme is null")).isZero();
        assertThat(count("destination.destination_categories"))
                .isEqualTo(nested(DestinationSeeder.class, "destinations.json", "categorySlugs"));
        assertThat(count("destination.destination_tags")).isEqualTo(nested(DestinationSeeder.class, "destinations.json", "tagSlugs"));
        assertThat(count("destination.destination_interests"))
                .isEqualTo(nested(DestinationSeeder.class, "destinations.json", "interestSlugs"));
        // A shipped place is in every tourism region one of its categories is shelved in.
        assertThat(count("destination.destination_regions")).isPositive().isEqualTo(tourismShelvings());
        assertThat(count("taxonomy.corridor_days"))
                .isEqualTo(nested(CorridorItinerarySeeder.class, "corridor-itineraries.json", "days"));
        assertThat(count("destination.corridor_destinations")).isEqualTo(corridorStops());
        assertThat(count("taxonomy.corridors where status = 'PUBLISHED' and duration_days is not null"))
                .isEqualTo(rows(CorridorSeeder.class, "corridors.json").size());
        assertThat(count("media.media_items where owner_type = 'DESTINATION'")).isEqualTo(nested(MinioConfig.class, "destination-media.json", "media"));

        // What the changelog ships, the test fixture replays: roles, badges and the season calendar.
        assertThat(count("auth.roles")).isEqualTo(6);
        assertThat(count("gamification.badges")).isEqualTo(12);
        assertThat(count("gamification.seasons")).isEqualTo(4);
    }

    /** What startup gets back from storing the seed photographs, without a MinIO to store them in. */
    private static Map<String, String> seedImageKeys() throws IOException {
        Map<String, String> keys = new HashMap<>();
        for (Resource image : new PathMatchingResourcePatternResolver()
                .getResources("classpath:ams/asmtravel/api/services/media/config/images/*")) {
            keys.put(image.getFilename(),
                    "general/" + new UUID(0L, 0L) + "/" + image.getFilename().toLowerCase(Locale.ROOT));
        }
        return keys;
    }

    private long count(String table) {
        Long count = jdbc.queryForObject("select count(*) from " + table, Long.class);
        return count == null ? 0 : count;
    }

    private List<JsonNode> rows(Class<?> seeder, String file) {
        return SeedJson.rows(objectMapper, seeder, file);
    }

    /** Every stop of every seeded day. */
    private long corridorStops() {
        long stops = 0;
        for (JsonNode itinerary : rows(CorridorItinerarySeeder.class, "corridor-itineraries.json")) {
            for (JsonNode day : SeedJson.array(itinerary, "days")) {
                for (JsonNode ignored : SeedJson.array(day, "stops")) {
                    stops++;
                }
            }
        }
        return stops;
    }

    /** The (place, tourism region) pairs that share a category, read from the seed files. */
    private long tourismShelvings() {
        List<JsonNode> themes = rows(RegionSeeder.class, "regions.json").stream()
                .filter(region -> "TOURISM".equals(SeedJson.text(region, "kind")))
                .toList();
        long pairs = 0;
        for (JsonNode place : rows(DestinationSeeder.class, "destinations.json")) {
            Set<String> categories = new HashSet<>(SeedJson.strings(place, "categorySlugs"));
            for (JsonNode theme : themes) {
                if (SeedJson.strings(theme, "categories").stream().anyMatch(categories::contains)) {
                    pairs++;
                }
            }
        }
        return pairs;
    }

    /** How many entries the rows' {@code field} arrays hold together: slugs, or a gallery's media. */
    private long nested(Class<?> seeder, String file, String field) {
        long entries = 0;
        for (JsonNode row : rows(seeder, file)) {
            for (JsonNode ignored : SeedJson.array(row, field)) {
                entries++;
            }
        }
        return entries;
    }
}
