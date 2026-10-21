package ams.asmtravel.api.services.search.domain;

import java.util.Map;
import java.util.UUID;

/**
 * The names a Khmer card prints beside ids: only the Khmer ones written, and the English categories to fall back on.
 *
 * @param provincesKm  province id to its Khmer name
 * @param categories   category id to its English name
 * @param categoriesKm category id to its Khmer name
 */
public record KhmerNames(Map<UUID, String> provincesKm, Map<UUID, String> categories, Map<UUID, String> categoriesKm) {
}
