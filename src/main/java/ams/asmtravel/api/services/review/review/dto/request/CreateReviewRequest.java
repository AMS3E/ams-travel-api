package ams.asmtravel.api.services.review.review.dto.request;

import ams.asmtravel.api.services.review.review.domain.ReviewTargetType;
import ams.asmtravel.api.services.review.review.domain.ReviewVisitType;
import jakarta.validation.constraints.PastOrPresent;
import java.time.YearMonth;
import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** A new review; the author comes from the sign-in. */
public record CreateReviewRequest(
        @NotNull ReviewTargetType targetType,
        @NotNull UUID targetId,
        @Min(1) @Max(5) Integer rating,
        @Size(max = 200) String title,
        @Size(max = 5000) String content,
        ReviewVisitType visitType,
        @PastOrPresent(message = "The visit cannot be in the future") YearMonth visitedOn,
        @Size(max = 6, message = "Up to six photos")
        List<@MediaKey(message = "Upload the photo first, then send its key") String> photoKeys
) {
}
