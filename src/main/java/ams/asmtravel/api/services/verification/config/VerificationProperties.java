package ams.asmtravel.api.services.verification.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * {@code app.verification.*}: when the travellers' reviews verify a place on their own.
 *
 * @param travelerSeal the Recommended by Traveler threshold
 */
@Validated
@ConfigurationProperties(prefix = "app.verification")
public record VerificationProperties(@Valid TravelerSeal travelerSeal) {

    public VerificationProperties {
        travelerSeal = travelerSeal == null ? new TravelerSeal(null, null) : travelerSeal;
    }

    /**
     * @param minRatings how many star ratings a place needs; every review carries one
     * @param minAverage the average those ratings have to reach
     */
    public record TravelerSeal(@Min(1) Integer minRatings, @DecimalMin("1") @DecimalMax("5") BigDecimal minAverage) {

        public TravelerSeal {
            minRatings = minRatings == null ? 10_000 : minRatings;
            minAverage = minAverage == null ? new BigDecimal("4.5") : minAverage;
        }

        /** Whether this many ratings at this average earn the seal. */
        public boolean earned(long ratings, BigDecimal average) {
            return ratings >= minRatings && average != null && average.compareTo(minAverage) >= 0;
        }
    }
}
