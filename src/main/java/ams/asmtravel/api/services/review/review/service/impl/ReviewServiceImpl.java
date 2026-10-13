package ams.asmtravel.api.services.review.review.service.impl;

import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.corridor.domain.CorridorDeletingEvent;
import ams.asmtravel.api.services.corridor.service.CorridorService;
import ams.asmtravel.api.services.point.service.PointService;
import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.destination.food.domain.FoodDeletingEvent;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.stay.domain.StayDeletingEvent;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.media.dto.response.MediaResponse;
import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;
import ams.asmtravel.api.services.review.rating.dto.response.RatingSummaryResponse;
import ams.asmtravel.api.services.review.rating.service.RatingService;
import ams.asmtravel.api.services.review.review.domain.Review;
import ams.asmtravel.api.services.review.review.domain.ReviewStatus;
import ams.asmtravel.api.services.review.review.domain.ReviewTargetType;
import ams.asmtravel.api.services.review.review.dto.request.CreateReviewRequest;
import ams.asmtravel.api.services.review.review.dto.request.UpdateReviewRequest;
import ams.asmtravel.api.services.review.review.dto.response.ReviewResponse;
import ams.asmtravel.api.services.review.review.dto.response.ReviewSummaryResponse;
import ams.asmtravel.api.services.review.review.exception.InvalidReviewException;
import ams.asmtravel.api.services.review.review.exception.ReviewAlreadyExistsException;
import ams.asmtravel.api.services.review.review.exception.ReviewNotFoundException;
import ams.asmtravel.api.services.review.review.mapper.ReviewMapper;
import ams.asmtravel.api.services.review.review.repository.ReviewRepository;
import ams.asmtravel.api.services.review.review.service.ReviewService;
import ams.asmtravel.api.services.review.review.specification.ReviewSpecification;
import ams.asmtravel.api.services.point.domain.PointSourceType;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import ams.asmtravel.api.services.user.user.domain.UserSummary;
import java.time.LocalDate;
import java.time.YearMonth;
import ams.asmtravel.api.services.user.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ams.asmtravel.api.services.authorization.domain.Permissions.FULL_CONTROL;
import static ams.asmtravel.api.services.authorization.domain.Permissions.REVIEW_MODERATE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    /** What a moderator may set; DELETED comes only from deleting. */
    private static final Set<ReviewStatus> MODERATION = EnumSet.of(
            ReviewStatus.PENDING, ReviewStatus.PUBLISHED, ReviewStatus.HIDDEN, ReviewStatus.REJECTED);

    private final ReviewRepository reviewRepository;
    private final MediaService photoService;
    private final RatingService ratingService;
    private final PointService pointService;
    private final DestinationService destinationService;
    private final StayService stayService;
    private final FoodService foodService;
    private final CorridorService corridorService;
    private final UserService userService;
    private final ReviewMapper mapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAll(ReviewTargetType targetType, UUID targetId, ReviewStatus status, UUID userId,
                                       Pageable pageable) {
        boolean onePlace = targetType != null && targetId != null;
        if (isModerator()) {
            return respond(reviewRepository.findAll(ReviewSpecification.of(targetType, targetId, status, userId), pageable));
        }
        if (!onePlace) {
            if (!(Authorities.current() instanceof JwtAuthenticationToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in as a moderator to see every review");
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a moderator sees every review; name a place");
        }
        return respond(reviewRepository.findAll(ReviewSpecification.publishedOf(targetType, targetId), pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewSummaryResponse> summaries(ReviewTargetType targetType, List<UUID> targetIds) {
        if (targetType == null || targetIds == null || targetIds.isEmpty() || targetIds.size() > 100) {
            throw new InvalidReviewException("Name a targetType and 1 to 100 targetIds");
        }
        Set<UUID> ids = new LinkedHashSet<>(targetIds);
        return summarize(targetType, ids, reviewRepository.summarize(targetType, ids),
                ratingService.summaries(ratingTarget(targetType), List.copyOf(ids)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewSummaryResponse> summariesBetween(ReviewTargetType targetType, List<UUID> targetIds, Instant from, Instant to) {
        if (targetType == null || targetIds == null || targetIds.isEmpty() || targetIds.size() > 100) {
            throw new InvalidReviewException("Name a targetType and 1 to 100 targetIds");
        }
        Set<UUID> ids = new LinkedHashSet<>(targetIds);
        return summarize(targetType, ids, reviewRepository.summarizeBetween(targetType, ids, from, to),
                ratingService.summariesBetween(ratingTarget(targetType), List.copyOf(ids), from, to));
    }

    /** One summary per place asked for, in that order, from the grouped review rows and the rating summaries. */
    private static List<ReviewSummaryResponse> summarize(ReviewTargetType targetType, Set<UUID> ids, List<Object[]> rows,
                                                         List<RatingSummaryResponse> ratingSummaries) {
        Map<UUID, Object[]> reviews = rows.stream().collect(Collectors.toMap(row -> (UUID) row[0], Function.identity()));
        Map<UUID, RatingSummaryResponse> ratings = ratingSummaries.stream()
                .collect(Collectors.toMap(RatingSummaryResponse::targetId, Function.identity()));
        return ids.stream().map(id -> {
            Object[] row = reviews.get(id);
            RatingSummaryResponse rating = ratings.get(id);
            return new ReviewSummaryResponse(targetType, id,
                    row == null ? 0 : (Long) row[1], row == null ? null : (Instant) row[2],
                    rating == null ? null : rating.average(), rating == null ? 0 : rating.count());
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse findById(UUID id) {
        Review review = find(id);
        boolean mayRead = review.getStatus() == ReviewStatus.PUBLISHED || isModerator()
                || (review.getStatus() != ReviewStatus.DELETED && isAuthor(review));
        if (!mayRead) {
            throw new ReviewNotFoundException();
        }
        return respond(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> findMine(UUID userId, Pageable pageable) {
        return respond(reviewRepository.findByUserIdAndStatusNot(userId, ReviewStatus.DELETED, pageable));
    }

    @Override
    @Transactional
    public ReviewResponse create(UUID userId, CreateReviewRequest request) {
        if (!isPublished(request.targetType(), request.targetId())) {
            throw new ReviewNotFoundException("Nothing published to review there");
        }
        Review review = reviewRepository.findByUserIdAndTargetTypeAndTargetId(userId, request.targetType(), request.targetId())
                .map(existing -> {
                    if (existing.getStatus() != ReviewStatus.DELETED) {
                        throw new ReviewAlreadyExistsException();
                    }
                    return existing;
                })
                .orElseGet(Review::new);
        review.setUserId(userId);
        review.setTargetType(request.targetType());
        review.setTargetId(request.targetId());
        review.setTitle(clean(request.title()));
        review.setContent(clean(request.content()));
        review.setVisitType(request.visitType());
        review.setVisitedOn(firstDay(request.visitedOn()));
        review.setStatus(ReviewStatus.PUBLISHED);
        review.setModeratedBy(null);
        review.setModeratedAt(null);
        review.setModerationNote(null);
        review = reviewRepository.saveAndFlush(review);
        photoService.replaceReviewPhotos(review.getId(), request.photoKeys() == null ? List.of() : request.photoKeys());
        if (request.rating() != null) {
            ratingService.rate(userId, ratingTarget(review.getTargetType()), review.getTargetId(), request.rating());
        }
        pointService.awardDailyTargetPoint(userId, review.getTargetType().name(), review.getTargetId(),
                PointSourceType.REVIEW, review.getId());
        events.publishEvent(new ReviewService.ReviewPublishedEvent(userId, review.getId()));
        log.info("Review {} written on {} {}", review.getId(), review.getTargetType(), review.getTargetId());
        return respond(review);
    }

    @Override
    @Transactional
    public ReviewResponse update(UUID id, UpdateReviewRequest request, UUID callerId) {
        Review review = find(id);
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new ReviewNotFoundException();
        }
        boolean author = Objects.equals(review.getUserId(), callerId);
        boolean moderator = isModerator();
        if (request.status() != null) {
            if (!moderator) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only a moderator changes a review's status");
            }
            moderate(review, request, callerId);
        } else if (author) {
            edit(review, request);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, moderator
                    ? "Say which status the review gets"
                    : "Only its author or a moderator changes a review");
        }
        return respond(reviewRepository.saveAndFlush(review));
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID callerId) {
        Review review = find(id);
        if (review.getStatus() == ReviewStatus.DELETED) {
            throw new ReviewNotFoundException();
        }
        if (!Objects.equals(review.getUserId(), callerId) && !isModerator()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only its author or a moderator deletes a review");
        }
        review.setStatus(ReviewStatus.DELETED);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewService.ReviewTotals totals(Instant since) {
        long total = reviewRepository.countByStatusNot(ReviewStatus.DELETED);
        long published = reviewRepository.countByStatus(ReviewStatus.PUBLISHED);
        return new ReviewService.ReviewTotals(total, published, total - published,
                since == null ? 0 : reviewRepository.countByStatusNotAndCreatedAtGreaterThanEqual(ReviewStatus.DELETED, since),
                ratingService.averageStars());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> findRecent() {
        return respond(reviewRepository.findTop5ByStatusNotOrderByCreatedAtDesc(ReviewStatus.DELETED));
    }

    @Override
    @Transactional(readOnly = true)
    public long countPublishedByUserBetween(UUID userId, Instant from, Instant to) {
        return reviewRepository.countByUserIdAndStatusAndCreatedAtBetween(userId, ReviewStatus.PUBLISHED, from, to);
    }

    // A person's or a place's reviews go with them.

    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        removeAll(reviewRepository.findByUserId(event.userId()));
    }

    @EventListener
    public void onDestinationDeleting(DestinationDeletingEvent event) {
        removeAll(reviewRepository.findByTargetTypeAndTargetId(ReviewTargetType.DESTINATION, event.destinationId()));
    }

    @EventListener
    public void onStayDeleting(StayDeletingEvent event) {
        removeAll(reviewRepository.findByTargetTypeAndTargetId(ReviewTargetType.STAY, event.stayId()));
    }

    @EventListener
    public void onFoodDeleting(FoodDeletingEvent event) {
        removeAll(reviewRepository.findByTargetTypeAndTargetId(ReviewTargetType.FOOD, event.foodId()));
    }

    @EventListener
    public void onCorridorDeleting(CorridorDeletingEvent event) {
        removeAll(reviewRepository.findByTargetTypeAndTargetId(ReviewTargetType.CORRIDOR, event.corridorId()));
    }

    /** The author's words, stars and photos; a null field keeps its value. */
    private void edit(Review review, UpdateReviewRequest request) {
        if (request.title() != null) {
            review.setTitle(clean(request.title()));
        }
        if (request.content() != null) {
            review.setContent(clean(request.content()));
        }
        if (request.visitType() != null) {
            review.setVisitType(request.visitType());
        }
        if (request.visitedOn() != null) {
            review.setVisitedOn(firstDay(request.visitedOn()));
        }
        if (request.photoKeys() != null) {
            photoService.replaceReviewPhotos(review.getId(), request.photoKeys());
        }
        if (request.rating() != null) {
            ratingService.rate(review.getUserId(), ratingTarget(review.getTargetType()), review.getTargetId(), request.rating());
        }
    }

    /** A moderator's decision; publishing again never earns another point. */
    private void moderate(Review review, UpdateReviewRequest request, UUID moderatorId) {
        if (!MODERATION.contains(request.status())) {
            throw new InvalidReviewException("A moderator sets PENDING, PUBLISHED, HIDDEN or REJECTED; deleting is DELETE");
        }
        review.setStatus(request.status());
        review.setModeratedBy(moderatorId);
        review.setModeratedAt(Instant.now());
        review.setModerationNote(clean(request.moderationNote()));
    }

    /** Reviews and their photos, for good. */
    private void removeAll(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return;
        }
        photoService.deleteByReviews(reviews.stream().map(Review::getId).toList());
        reviewRepository.deleteAll(reviews);
        reviewRepository.flush();
    }

    private Page<ReviewResponse> respond(Page<Review> page) {
        return new PageImpl<>(respond(page.getContent()), page.getPageable(), page.getTotalElements());
    }

    private ReviewResponse respond(Review review) {
        return respond(List.of(review)).getFirst();
    }

    /** Authors, photos and stars are read once for the whole list, never per review. */
    private List<ReviewResponse> respond(List<Review> reviews) {
        if (reviews.isEmpty()) {
            return List.of();
        }
        Map<UUID, UserSummary> authors = userService.findSummaries(reviews.stream().map(Review::getUserId).toList());
        Map<UUID, List<MediaResponse>> photos = photoService.findByReviews(reviews.stream().map(Review::getId).toList());
        Map<ReviewTargetType, Map<RatingService.Rater, Integer>> stars = new EnumMap<>(ReviewTargetType.class);
        reviews.stream().collect(Collectors.groupingBy(Review::getTargetType)).forEach((type, ofType) ->
                stars.put(type, ratingService.findStars(ratingTarget(type), ofType.stream().map(ReviewServiceImpl::rater).toList())));
        return reviews.stream()
                .map(review -> mapper.toResponse(review, authors.get(review.getUserId()),
                        photos.getOrDefault(review.getId(), List.of()),
                        stars.getOrDefault(review.getTargetType(), new HashMap<>()).get(rater(review))))
                .toList();
    }

    private boolean isPublished(ReviewTargetType targetType, UUID targetId) {
        return switch (targetType) {
            case DESTINATION -> destinationService.isPublished(targetId);
            case STAY -> stayService.isPublished(targetId);
            case FOOD -> foodService.isPublished(targetId);
            case CORRIDOR -> corridorService.isPublished(targetId);
        };
    }

    private boolean isAuthor(Review review) {
        return userService.findUserId(Authorities.current()).map(id -> id.equals(review.getUserId())).orElse(false);
    }

    private static boolean isModerator() {
        return Authorities.hasAny(Authorities.current(), REVIEW_MODERATE, FULL_CONTROL);
    }

    private static RatingTargetType ratingTarget(ReviewTargetType target) {
        return RatingTargetType.valueOf(target.name());
    }

    private static RatingService.Rater rater(Review review) {
        return new RatingService.Rater(review.getUserId(), review.getTargetId());
    }

    /** The month of the visit is kept as its first day. */
    private static LocalDate firstDay(YearMonth month) {
        return month == null ? null : month.atDay(1);
    }

    private static String clean(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private Review find(UUID id) {
        return reviewRepository.findById(id).orElseThrow(ReviewNotFoundException::new);
    }
}
