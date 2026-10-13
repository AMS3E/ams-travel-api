package ams.asmtravel.api.support;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The reference rows the changelog ships, replayed into the tables Hibernate
 * made: the six roles and their permissions, the twelve badges, the season
 * calendar, the amenities taxonomy. Liquibase is off in the test context (see test application.yml),
 * so without this every role lookup finds nothing.
 *
 * <p>RECONSTRUCTED on 2026-09-30 (see {@link Callers}). The changelog says how
 * it worked: "the test run replays these statements against tables Hibernate
 * made" (auth/002, gamification/008, gamification/009). This runs every
 * {@code sql} change of the listed changesets, in changelog order, with the
 * H2 value of {@code ${fn.uuid}}; rollback blocks are skipped. Runs once per
 * Spring context and only while {@code auth.roles} is empty.
 */
@Component
public class ReferenceRows implements ApplicationRunner {

    /** Changelog files whose data statements make up the reference rows, in the order the master applies them. */
    static final List<String> FILES = List.of(
            "db/changelog/v2/auth/002-roles-and-permissions.yaml",
            "db/changelog/v2/auth/003-partner-manage.yaml",
            "db/changelog/v2/auth/004-editor-bdo-publish.yaml",
            "db/changelog/v2/auth/005-drop-partner-manage.yaml",
            "db/changelog/v2/auth/006-contributor-listings.yaml",
            "db/changelog/v2/auth/007-verify-bdo-admin.yaml",
            "db/changelog/v2/gamification/008-badge-rows.yaml",
            "db/changelog/v2/gamification/009-season-rows.yaml",
            "db/changelog/v2/taxonomy/013-amenities.yaml");

    private final JdbcTemplate jdbc;

    public ReferenceRows(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        Integer roles = jdbc.queryForObject("select count(*) from auth.roles", Integer.class);
        if (roles != null && roles > 0) {
            return;
        }
        for (String file : FILES) {
            for (String statement : statements(file)) {
                jdbc.execute(statement);
            }
        }
    }

    /** Every statement of every {@code sql} change in the file, as H2 runs it. */
    @SuppressWarnings("unchecked")
    static List<String> statements(String file) throws IOException {
        List<String> out = new ArrayList<>();
        try (InputStream in = new ClassPathResource(file).getInputStream()) {
            Map<String, Object> root = new Yaml().load(in);
            for (Object entry : (List<Object>) root.get("databaseChangeLog")) {
                Map<String, Object> changeSet = (Map<String, Object>) ((Map<String, Object>) entry).get("changeSet");
                if (changeSet == null || "postgresql".equals(changeSet.get("dbms"))) {
                    continue;
                }
                for (Object change : (List<Object>) changeSet.getOrDefault("changes", List.of())) {
                    Map<String, Object> sql = (Map<String, Object>) ((Map<String, Object>) change).get("sql");
                    if (sql == null) {
                        continue;
                    }
                    String text = String.valueOf(sql.get("sql")).replace("${fn.uuid}", "random_uuid()");
                    boolean split = !Boolean.FALSE.equals(sql.get("splitStatements"));
                    for (String piece : split ? text.split(";\\s*(\\n|$)") : new String[] {text}) {
                        if (!piece.isBlank()) {
                            out.add(piece.trim());
                        }
                    }
                }
            }
        }
        return out;
    }
}
