package ams.asmtravel.api.services.admin.mapper;

import ams.asmtravel.api.services.admin.dto.response.OverviewResponse;
import ams.asmtravel.api.services.review.review.service.ReviewService.ReviewTotals;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AdminMapper {

    /** @param seasonRunning whether a season is active; the season's count is null otherwise */
    default OverviewResponse.Reviews toReviews(ReviewTotals totals, boolean seasonRunning) {
        return new OverviewResponse.Reviews(totals.total(), totals.visible(), totals.hidden(),
                seasonRunning ? totals.since() : null, totals.averageRating());
    }
}
