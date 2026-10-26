package ams.asmtravel.api.services.search.dto.request;

import ams.asmtravel.api.services.search.domain.SearchType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The app's search box and filter chips, as query parameters; every one is optional.
 *
 * @param type                what to search (destinations when left out)
 * @param q                   words to look for
 * @param regionId            one region; {@code regionIds} takes several. A zone matches the places in its provinces,
 *                            a tourism region the attractions shelved in it and the corridors stopping at them
 * @param provinceId          one province; {@code provinceIds} takes several. Corridors match through their stops
 * @param categoryIds         any of these categories (destinations, food)
 * @param tagIds              any of these tags (destinations)
 * @param interestIds         any of these interests (all kinds)
 * @param verified            true keeps only the verified places, of any kind of destination; corridors carry no badge and answer empty
 * @param near                a destination's id: what lies around it, nearest first, instead of A to Z. Stays and food
 *                            within the Nearby radius, attractions within 25 km, corridors stopping there
 * @param radiusKm            how far {@code near} reaches for attractions, 0.1 to 50 km; 25 when left out
 * @param lang                {@code km} for the cards' text in Khmer where it is written; English when left out
 */
public record SearchRequest(
        SearchType type,
        @Size(max = 100) String q,
        UUID regionId,
        UUID provinceId,
        @Size(max = 20) Set<UUID> regionIds,
        @Size(max = 20) Set<UUID> provinceIds,
        @Size(max = 20) Set<UUID> categoryIds,
        @Size(max = 20) Set<UUID> tagIds,
        @Size(max = 20) Set<UUID> interestIds,
        Boolean verified,
        UUID near,
        @DecimalMin("0.1") @DecimalMax("50") Double radiusKm,
        @Pattern(regexp = "^(en|km)?$", message = "Use en or km") String lang
) {
    public SearchType typeOrDefault() {
        return type == null ? SearchType.DESTINATION : type;
    }

    /** The regions asked for, the single one and the list together; empty for no filter. */
    public Set<UUID> regions() {
        return together(regionId, regionIds);
    }

    public Set<UUID> provinces() {
        return together(provinceId, provinceIds);
    }

    /** The attraction radius asked for, else the given default. */
    public double radiusKmOr(double fallback) {
        return radiusKm == null ? fallback : radiusKm;
    }

    public boolean khmer() {
        return "km".equals(lang);
    }

    public boolean verifiedOnly() {
        return Boolean.TRUE.equals(verified);
    }

    private static Set<UUID> together(UUID one, Set<UUID> several) {
        Set<UUID> all = new HashSet<>();
        if (several != null) {
            all.addAll(several);
        }
        if (one != null) {
            all.add(one);
        }
        return all;
    }
}
