package ams.asmtravel.api.services.admin.dto.response;

import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.verification.domain.VerificationBasis;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Every destination's standing with travellers and with the desks, for the
 * dashboard's Destination Stats page: one read, every status.
 *
 * @param browseDays how many days the browses are counted over, today included
 */
public record DestinationStatsResponse(int browseDays, List<Place> places) {

    /**
     * @param browses             different signed-in visitors who opened it in the window
     * @param viewCount           first opens of a day, all time, signed in or not
     * @param ratingCount         star ratings so far; every review carries one
     * @param averageRating       their average, one decimal; null while unrated
     * @param reviewCount         published reviews
     * @param verified            by a desk's decision or by the reviews
     * @param basis               what the verification rests on; null while not verified
     * @param travelerRecommended whether the reviews have earned the Recommended by Traveler seal
     */
    public record Place(
            UUID id,
            String name,
            String slug,
            UUID provinceId,
            ListingStatus status,
            long browses,
            long viewCount,
            long ratingCount,
            BigDecimal averageRating,
            long reviewCount,
            boolean verified,
            VerificationBasis basis,
            boolean travelerRecommended
    ) {
    }
}
