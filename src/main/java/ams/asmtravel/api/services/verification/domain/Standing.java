package ams.asmtravel.api.services.verification.domain;

import java.math.BigDecimal;

/**
 * Where a place stands with the travellers: its star ratings, and whether
 * they have earned it the Recommended by Traveler seal.
 */
public record Standing(long ratingCount, BigDecimal averageRating, boolean travelerRecommended) {

    public static final Standing NONE = new Standing(0, null, false);
}
