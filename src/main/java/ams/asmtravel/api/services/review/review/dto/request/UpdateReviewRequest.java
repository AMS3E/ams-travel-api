package ams.asmtravel.api.services.review.review.dto.request;

import ams.asmtravel.api.services.review.review.domain.ReviewStatus;
import ams.asmtravel.api.services.media.dto.request.MediaKey;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Authors change the text, moderators the status; null keeps a value. */
public record UpdateReviewRequest(
        @Min(1) @Max(5) Integer rating,
        @Size(max = 200) String title,
        @Size(max = 5000) String content,
        @Size(max = 6, message = "Up to six photos")
        List<@MediaKey(message = "Upload the photo first, then send its key") String> photoKeys,
        ReviewStatus status,
        @Size(max = 500) String moderationNote
) {
}
