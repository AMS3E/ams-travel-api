package ams.asmtravel.api.services.review.review.service;

import ams.asmtravel.api.services.review.review.dto.request.CreateReviewRequest;
import ams.asmtravel.api.services.review.review.dto.request.UpdateReviewRequest;
import ams.asmtravel.api.services.review.review.dto.response.ReviewResponse;
import ams.asmtravel.api.services.review.review.dto.response.ReviewSummaryResponse;
import ams.asmtravel.api.services.review.review.domain.ReviewStatus;
import ams.asmtravel.api.services.review.review.domain.ReviewTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** What customers wrote about places, and moderating it; stars are the rating service's. */
public interface ReviewService {

    /** Published when a visitor writes a review: the badge service counts it towards Reviewer. */
    record ReviewPublishedEvent(UUID userId, UUID reviewId) {
    }

    /** Review counts for the admin overview. */
    record ReviewTotals(long total, long visible, long hidden, long since, BigDecimal averageRating) {
    }

    /** A page of reviews; only moderators may list beyond one place's published ones. */
    /** {@code stars}: only reviews whose author rated the place with one of these; null or empty for all. */
    Page<ReviewResponse> getAll(ReviewTargetType targetType, UUID targetId, ReviewStatus status, UUID userId,
                                Set<Integer> stars, Pageable pageable);

    /** Published review count and newest date per place, with the place's rating. */
    List<ReviewSummaryResponse> summaries(ReviewTargetType targetType, List<UUID> targetIds);

    /** The same, counting only the reviews written and the stars given in a window; {@code to} exclusive. */
    List<ReviewSummaryResponse> summariesBetween(ReviewTargetType targetType, List<UUID> targetIds, Instant from, Instant to);

    /** A published review for anybody; any other for its author or a moderator. */
    ReviewResponse findById(UUID id);

    /** The caller's own reviews, whatever their status except deleted. */
    Page<ReviewResponse> findMine(UUID userId, Set<Integer> stars, Pageable pageable);

    /** Reviews a published place, published at once, and earns the day's point for it when still due. */
    ReviewResponse create(UUID userId, CreateReviewRequest request);

    /** The author edits the words; a moderator sets the status. Neither earns another point. */
    ReviewResponse update(UUID id, UpdateReviewRequest request, UUID callerId);

    /** Its author or a moderator; the review leaves the site but stays on record as DELETED. */
    void delete(UUID id, UUID callerId);

    ReviewTotals totals(Instant since);

    List<ReviewResponse> findRecent();

    /** A person's published reviews written in a window: what the Reviewer badge counts. */
    long countPublishedByUserBetween(UUID userId, Instant from, Instant to);
}
