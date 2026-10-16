package ams.asmtravel.api.services.verification.domain;

import java.math.BigDecimal;

/** Where the Traveler seal's bar stands: this many star ratings at this average or better. */
public record TravelerSealBar(long minRatings, BigDecimal minAverage) {
}
