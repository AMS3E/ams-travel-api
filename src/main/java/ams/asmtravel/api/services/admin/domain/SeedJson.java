package ams.asmtravel.api.services.admin.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Reads a service's seed file from that service's own package. */
@Slf4j
public final class SeedJson {

    private SeedJson() {
    }

    /** The rows in {@code fileName} from {@code owner}'s package; a missing file gives no rows, not an error. */
    public static List<JsonNode> rows(ObjectMapper objectMapper, Class<?> owner, String fileName) {
        ClassPathResource resource = new ClassPathResource(fileName, owner);
        if (!resource.exists()) {
            log.warn("{} not found beside {}, seeding nothing", fileName, owner.getSimpleName());
            return List.of();
        }
        JsonNode root;
        try (InputStream in = resource.getInputStream()) {
            root = objectMapper.readTree(in);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read seed file " + fileName, exception);
        }
        if (!root.isArray()) {
            log.warn("{} does not hold an array, seeding nothing", fileName);
            return List.of();
        }
        List<JsonNode> rows = new ArrayList<>(root.size());
        root.forEach(rows::add);
        return rows;
    }

    public static Iterable<JsonNode> array(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.isArray() ? List.of() : value;
    }

    public static List<String> strings(JsonNode node, String field) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : array(node, field)) {
            values.add(item.asString());
        }
        return values;
    }

    public static String text(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asString() : null;
    }

    public static String textOr(JsonNode node, String field, String fallback) {
        String value = text(node, field);
        return value == null ? fallback : value;
    }

    public static Integer integer(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asInt() : null;
    }

    public static Double decimal(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asDouble() : null;
    }
}
