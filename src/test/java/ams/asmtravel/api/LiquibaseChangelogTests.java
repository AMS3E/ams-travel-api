package ams.asmtravel.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the whole changelog, which the rest of the suite never does: every
 * other context builds its tables from the entities with Liquibase off, so a
 * broken or unregistered changeset would otherwise reach production
 * unopposed. PostgreSQL-only changesets (the expression indexes) are skipped
 * here; agreement between changelog and entities is still only proven against
 * a real PostgreSQL with {@code ddl-auto: validate}.
 */
@SpringBootTest(properties = {
        "spring.liquibase.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        // Its own database: the others are built from the entities with create-drop.
        "spring.datasource.url=jdbc:h2:mem:ams_liquibase;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class LiquibaseChangelogTests {

    @Autowired JdbcTemplate jdbc;

    @Test
    void theErdsTablesAndTheRestoredOnesAreCreated() {
        List<String> tables = jdbc.queryForList("""
                select lower(table_schema) || '.' || lower(table_name) from information_schema.tables
                where lower(table_schema) in ('auth', 'taxonomy', 'destination', 'publishing',
                                              'engagement', 'gamification', 'system', 'history', 'media')
                """, String.class);
        // The ERD less the two partner tables it dissolved into the listings, plus
        // seasons, verifications, the history schema, the two region link tables, a
        // region's historical coverage, a stay's gallery, a destination's tourism regions, a corridor's days,
        // a stay's interests, its rooms, a food listing's menu, a tourism region's interests,
        // an ancient site's historical periods, and a corridor's interests; the four galleries became one media table, and ratings got their own; points and season progress are three more, and six badge tables became three.
        // The amenities taxonomy is one more; system.seed_runs (2026-10-14) another.
        assertThat(tables).hasSize(41 - 2 + 1 + 1 + 8 + 2 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 + 1 - 4 + 1 + 1 + 3 - 3 + 1 + 1);
        assertThat(tables).contains("taxonomy.region_interests", "history.destination_periods", "taxonomy.corridor_interests",
                "taxonomy.amenities");
        assertThat(tables).contains("publishing.destination_verifications", "gamification.seasons",
                "history.destination_history", "system.audit_logs", "engagement.digital_stamps",
                "destination.destination_stay_interests", "destination.destination_stay_rooms",
                "destination.destination_food_menu_items", "media.media_items", "engagement.ratings");
    }

    /**
     * Every {@code PERM_*} the API is written in terms of, read from the class
     * that declares them.
     *
     * <p>A permission stored but never checked reads like a rule that is
     * enforced; one checked but never stored locks people out of an endpoint
     * that looks open. Neither is visible from a row count, so this compares
     * the two lists rather than counting either.</p>
     */
    private static List<String> permissionsTheApiChecks() throws IOException {
        String source = Files.readString(
                Path.of("src/main/java/ams/asmtravel/api/services/authorization/domain/Permissions.java"));
        Matcher constant = Pattern.compile("\"ROLE_(PERM_[A-Z_]+)\"").matcher(source);
        List<String> codes = new ArrayList<>();
        while (constant.find()) {
            codes.add(constant.group(1));
        }
        assertThat(codes).as("Permissions declares some").isNotEmpty();
        return codes;
    }

    @Test
    void theReferenceRowsShip() throws IOException {
        assertThat(jdbc.queryForObject("select count(*) from auth.roles", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForList("select code from auth.permissions", String.class))
                .containsExactlyInAnyOrderElementsOf(permissionsTheApiChecks());
        assertThat(jdbc.queryForObject("select count(*) from gamification.badges", Integer.class)).isEqualTo(12);
        assertThat(jdbc.queryForList("select kind from gamification.seasons order by starts_on", String.class))
                .containsExactly("HIGH_SEASON", "GREEN_SEASON", "HIGH_SEASON", "GREEN_SEASON");
    }

    /** A person's rows go with them; a verification decision keeps its decider. */
    @Test
    void crossSchemaKeysHold() {
        jdbc.update("insert into auth.users (id, email, full_name, status, created_at, updated_at) "
                + "values (random_uuid(), 'keys@test.test', 'Keys', 'ACTIVE', current_timestamp, current_timestamp)");
        String user = jdbc.queryForObject("select cast(id as varchar) from auth.users where email = 'keys@test.test'", String.class);
        jdbc.update("insert into system.notifications (id, user_id, title, type, is_read, created_at) "
                + "values (random_uuid(), cast(? as uuid), 'Hello', 'BADGE_EARNED', false, current_timestamp)", user);
        jdbc.update("delete from auth.users where id = cast(? as uuid)", user);
        assertThat(jdbc.queryForObject("select count(*) from system.notifications", Integer.class)).isZero();
    }
}
