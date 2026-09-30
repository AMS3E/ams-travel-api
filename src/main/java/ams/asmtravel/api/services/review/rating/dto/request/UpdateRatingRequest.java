package ams.asmtravel.api.services.review.rating.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** New stars for an existing rating. */
public record UpdateRatingRequest(@NotNull @Min(1) @Max(5) Integer stars) {
}
