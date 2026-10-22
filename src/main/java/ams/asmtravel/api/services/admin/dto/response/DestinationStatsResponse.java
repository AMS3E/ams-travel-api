package ams.asmtravel.api.services.admin.dto.response;

import ams.asmtravel.api.services.admin.domain.PlaceType;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.verification.domain.VerificationBasis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Every destination's standing with travellers and with the desks, for the
 * dashboard's Destination Stats page: one read, every status. Browses,
 * reviews and ratings are counted over the period asked for; views, the
 * Verified badge and the status are as of today whatever the period.
 *
 * @param from the period's first day in Cambodia, inclusive; null for all time
 * @param to   its last day, inclusive; null for up to today
 */
public record DestinationStatsResponse(LocalDate from, LocalDate to, List<Place> places) {

    /**
     * @param type                an attraction, a stay or a food place; this list holds attractions only
     * @param browses             different signed-in visitors who opened it in the period
     * @param viewCount           first opens of a day, all time, signed in or not
     * @param checkIns            travellers who scanned its code on site in the period; one per traveller, place and season
     * @param ratingCount         star ratings given in the period; every review carries one
     * @param averageRating       their average, one decimal; null when there are none
     * @param reviewCount         published reviews written in the period
     * @param verified            by a desk's decision or by the reviews
     * @param basis               what the verification rests on; null while not verified
     * @param travelerRecommended whether the reviews have earned the Recommended by Traveler seal
     */
    public record Place(
            UUID id,
            PlaceType type,
            String name,
            String slug,
            UUID provinceId,
            ListingStatus status,
            long browses,
            long viewCount,
            long checkIns,
            long ratingCount,
            BigDecimal averageRating,
            long reviewCount,
            boolean verified,
            VerificationBasis basis,
            boolean travelerRecommended
    ) {
    }
}
