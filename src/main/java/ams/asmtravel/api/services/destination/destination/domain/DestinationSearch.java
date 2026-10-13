package ams.asmtravel.api.services.destination.destination.domain;

import java.util.Set;
import java.util.UUID;

/**
 * What the app's search box and filter chips ask for; a null or empty value leaves that filter out.
 *
 * @param q           words looked for in the name, the short description and the address
 * @param regionId    a tourism region the place is shelved in
 * @param provinceId  the province it sits in
 * @param categoryIds any of these categories
 * @param tagIds      any of these tags
 * @param interestIds any of these interests
 */
public record DestinationSearch(String q, UUID regionId, UUID provinceId, Set<UUID> categoryIds, Set<UUID> tagIds,
                                Set<UUID> interestIds) {
}
