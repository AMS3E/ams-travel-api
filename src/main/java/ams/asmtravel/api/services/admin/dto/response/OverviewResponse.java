package ams.asmtravel.api.services.admin.dto.response;

import ams.asmtravel.api.services.review.review.dto.response.ReviewResponse;
import ams.asmtravel.api.services.season.dto.response.SeasonResponse;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Every dashboard tile in one response.
 *
 * @param counts        rows per table; full control only, left out otherwise
 * @param currentSeason the season running today; null between seasons
 */
public record OverviewResponse(
        @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Long> counts,
        Reviews reviews,
        SeasonResponse currentSeason,
        List<ReviewResponse> recentReviews
) {

    /**
     * @param thisSeason    written since the current season began; null between seasons
     * @param averageRating of the visible ones, one decimal; null when there are none
     */
    public record Reviews(long total, long visible, long hidden, Long thisSeason, BigDecimal averageRating) {
    }
}
