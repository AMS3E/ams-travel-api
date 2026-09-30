package ams.asmtravel.api.services.review.rating.dto.request;

import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Rating a place: one rating per person per place. */
public record CreateRatingRequest(
        @NotNull RatingTargetType targetType,
        @NotNull UUID targetId,
        @NotNull @Min(1) @Max(5) Integer stars
) {
}
