package ams.asmtravel.api.services.verification.domain;

import java.math.BigDecimal;

/**
 * Where a place stands with the travellers: its star ratings, and whether
 * they are enough to verify it on their own.
 */
public record Standing(long ratingCount, BigDecimal averageRating, boolean scoreVerified) {

    public static final Standing NONE = new Standing(0, null, false);
}
