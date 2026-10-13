package ams.asmtravel.api.services.search.dto.request;

import ams.asmtravel.api.services.search.domain.SearchType;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/**
 * The app's search box and filter chips, as query parameters; every one is optional.
 *
 * @param type        what to search (destinations when left out)
 * @param q           words to look for
 * @param regionId    only places shelved in this tourism region (destinations)
 * @param provinceId  only places in this province (destinations, stays, food)
 * @param categoryIds any of these categories (destinations, food)
 * @param tagIds      any of these tags (destinations)
 * @param interestIds any of these interests (all kinds)
 */
public record SearchRequest(
        SearchType type,
        @Size(max = 100) String q,
        UUID regionId,
        UUID provinceId,
        @Size(max = 20) Set<UUID> categoryIds,
        @Size(max = 20) Set<UUID> tagIds,
        @Size(max = 20) Set<UUID> interestIds
) {
    public SearchType typeOrDefault() {
        return type == null ? SearchType.DESTINATION : type;
    }
}
