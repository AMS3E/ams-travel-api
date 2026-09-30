package ams.asmtravel.api.services.review.rating.service;

import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;
import ams.asmtravel.api.services.review.rating.dto.request.CreateRatingRequest;
import ams.asmtravel.api.services.review.rating.dto.request.UpdateRatingRequest;
import ams.asmtravel.api.services.review.rating.dto.response.RatingResponse;
import ams.asmtravel.api.services.review.rating.dto.response.RatingSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** People's stars for places (destinations, stays, food listings), and what they add up to. */
public interface RatingService {

    /** Who rated which place: how a review finds its author's stars. */
    record Rater(UUID userId, UUID targetId) {
    }

    /** Active ratings, newest first; each filter may be left out. */
    Page<RatingResponse> getAll(RatingTargetType targetType, UUID targetId, Integer stars, Pageable pageable);

    /** Average, total and 1–5 distribution for 1 to 100 places, in the order asked, unrated ones included. */
    List<RatingSummaryResponse> summaries(RatingTargetType targetType, List<UUID> targetIds);

    RatingResponse findById(UUID id);

    /** The caller's active rating of a place; 404 when they have none. */
    RatingResponse findMine(UUID userId, RatingTargetType targetType, UUID targetId);

    /** Rates a published place and earns the day's point for it when not earned yet; 409 when already rated. */
    RatingResponse create(UUID userId, CreateRatingRequest request);

    /** New stars; its author only, and never another point. */
    RatingResponse update(UUID id, UpdateRatingRequest request, UUID callerId);

    /** Its author or a moderator; the rating stops counting but stays on record. */
    void delete(UUID id, UUID callerId);

    // For the review service.

    /** Creates or changes a person's rating of a place, as {@link #create} and {@link #update} would. */
    void rate(UUID userId, RatingTargetType targetType, UUID targetId, int stars);

    /** These people's active stars for these places of one type; a person with none is left out. */
    Map<Rater, Integer> findStars(RatingTargetType targetType, Collection<Rater> raters);

    /** The average of every active rating, one decimal; null when there are none. */
    BigDecimal averageStars();
}
