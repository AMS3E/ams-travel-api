package ams.asmtravel.api.services.destination.destination.domain;

import java.util.Set;
import java.util.UUID;

/**
 * What the app's search asks of published destinations; every part is optional.
 *
 * @param regionIds   any of these regions: a zone through the province, a tourism region through the shelf
 * @param provinceIds any of these provinces
 */
public record DestinationSearch(String q, Set<UUID> regionIds, Set<UUID> provinceIds, Set<UUID> categoryIds, Set<UUID> tagIds,
                                Set<UUID> interestIds) {
}
