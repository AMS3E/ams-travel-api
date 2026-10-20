package ams.asmtravel.api.services.admin.domain;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Rows seeded so far, found by the slug the seed files use (slugs are never stored). */
@Slf4j
public class CatalogSeedContext {

    private final Map<Class<?>, Map<String, Object>> published = new LinkedHashMap<>();
    private final Map<String, String> imageKeys;

    public CatalogSeedContext(Map<String, String> imageKeys) {
        this.imageKeys = imageKeys;
    }

    /** Records a row just written, under its seed slug. */
    public <T> void publish(Class<T> type, String slug, T entity) {
        published.computeIfAbsent(type, key -> new LinkedHashMap<>()).put(slug, entity);
    }

    /** The row that slug means, or null when nothing published one. */
    public <T> T find(Class<T> type, String slug) {
        Map<String, Object> rows = published.get(type);
        return rows == null ? null : type.cast(rows.get(slug));
    }

    /** The row that slug means; fails startup when missing, as half a catalogue is worse than none. */
    public <T> T require(Class<T> type, String slug, String kind) {
        T value = find(type, slug);
        if (value == null) {
            throw new IllegalStateException("Seed references unknown " + kind + " \"" + slug + "\"");
        }
        return value;
    }

    /** Every row of a kind published so far, in seed order. */
    public <T> List<T> all(Class<T> type) {
        Map<String, Object> rows = published.get(type);
        return rows == null ? List.of() : rows.values().stream().map(type::cast).toList();
    }

    /** The rows those slugs mean, skipping and reporting any that nothing published. */
    public <T> List<T> resolve(Class<T> type, List<String> slugs) {
        List<T> resolved = new ArrayList<>(slugs.size());
        for (String slug : slugs) {
            T value = find(type, slug);
            if (value == null) {
                log.warn("Seed references unknown {} {}", type.getSimpleName(), slug);
                continue;
            }
            resolved.add(value);
        }
        return resolved;
    }

    /** Object key of the photo this row names; null when the photo is missing, so no broken link. */
    public String imageKey(JsonNode node) {
        return imageKey(SeedJson.text(node, "image"));
    }

    /** The object key for a seed photograph named by file name, or null. */
    public String imageKey(String fileName) {
        if (fileName == null) {
            return null;
        }
        String key = imageKeys.get(fileName);
        if (key == null) {
            log.warn("Seed references unknown image {}", fileName);
        }
        return key;
    }
}
