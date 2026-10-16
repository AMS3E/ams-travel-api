package ams.asmtravel.api.services.search.dto.response;

import ams.asmtravel.api.services.search.domain.SearchType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * One row of a list in the app, complete: name, picture, where it is, what it is, how it is rated and
 * whether it is verified. Fields a kind does not have are null: a stay has no slug, a corridor no province
 * and no pin, only a stay or a food place has a price, only a corridor a duration.
 *
 * @param basis               OFFICIAL_SITE, PARTNER, PAID or TRAVELER_SCORE when verified; destinations only
 * @param travelerRecommended the "Recommended by Traveler" seal; destinations only
 */
public record SearchHitResponse(
        SearchType type,
        UUID id,
        String name,
        String slug,
        String shortDescription,
        String coverUrl,
        UUID provinceId,
        String provinceName,
        BigDecimal latitude,
        BigDecimal longitude,
        List<UUID> categoryIds,
        List<String> categoryNames,
        List<UUID> interestIds,
        List<UUID> tourismRegionIds,
        BigDecimal averageRating,
        long ratingCount,
        long reviewCount,
        boolean verified,
        String basis,
        boolean travelerRecommended,
        BigDecimal priceFrom,
        String currency,
        Integer durationDays,
        /** Food places: open at this moment in Cambodia (null when no hours are known) and today's line. */
        Boolean openNow,
        String todayHours,
        /** Corridors: the stops in travel order, and the whole route Google-encoded once calculated. */
        List<String> stopNames,
        String encodedPolyline,
        /** Attractions searched {@code near} a destination: metres from it, measured on the map; null otherwise. */
        Integer distanceMeters
) {

    /** The same card, with its distance from the place it was searched around. */
    public SearchHitResponse withDistanceMeters(Integer metres) {
        return new SearchHitResponse(type, id, name, slug, shortDescription, coverUrl, provinceId, provinceName, latitude, longitude,
                categoryIds, categoryNames, interestIds, tourismRegionIds, averageRating, ratingCount, reviewCount, verified, basis,
                travelerRecommended, priceFrom, currency, durationDays, openNow, todayHours, stopNames, encodedPolyline, metres);
    }
}
