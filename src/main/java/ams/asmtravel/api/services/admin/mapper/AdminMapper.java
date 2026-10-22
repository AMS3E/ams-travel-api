package ams.asmtravel.api.services.admin.mapper;

import ams.asmtravel.api.services.admin.dto.response.DestinationStatsResponse;
import ams.asmtravel.api.services.admin.dto.response.OverviewResponse;
import ams.asmtravel.api.services.admin.domain.PlaceSummary;
import ams.asmtravel.api.services.review.review.dto.response.ReviewSummaryResponse;
import ams.asmtravel.api.services.review.review.service.ReviewService.ReviewTotals;
import ams.asmtravel.api.services.verification.dto.response.VerificationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AdminMapper {

    /** @param seasonRunning whether a season is active; the season's count is null otherwise */
    default OverviewResponse.Reviews toReviews(ReviewTotals totals, boolean seasonRunning) {
        return new OverviewResponse.Reviews(totals.total(), totals.visible(), totals.hidden(),
                seasonRunning ? totals.since() : null, totals.averageRating());
    }

    /**
     * @param browses      null when nobody signed in opened it in the window
     * @param checkIns     null when nobody scanned its code on site in the window
     * @param review       null when it has no reviews and no stars
     * @param verification null when the verification service did not answer for it
     */
    default DestinationStatsResponse.Place toPlace(PlaceSummary place, Long browses, Long checkIns, ReviewSummaryResponse review,
                                                  VerificationResponse verification) {
        return new DestinationStatsResponse.Place(place.id(), place.type(), place.name(), place.slug(), place.provinceId(), place.status(),
                browses == null ? 0 : browses, place.viewCount(), checkIns == null ? 0 : checkIns,
                review == null ? 0 : review.ratingCount(), review == null ? null : review.averageRating(),
                review == null ? 0 : review.reviewCount(),
                verification != null && verification.verified(), verification == null ? null : verification.basis(),
                verification != null && Boolean.TRUE.equals(verification.travelerRecommended()));
    }
}
