package ams.asmtravel.api.services.review.rating.service.impl;

import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.authorization.domain.Permissions;
import ams.asmtravel.api.services.point.service.PointService;
import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.destination.food.domain.FoodDeletingEvent;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.stay.domain.StayDeletingEvent;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.corridor.domain.CorridorDeletingEvent;
import ams.asmtravel.api.services.corridor.service.CorridorService;
import ams.asmtravel.api.services.review.rating.domain.Rating;
import ams.asmtravel.api.services.review.rating.domain.RatingStatus;
import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;
import ams.asmtravel.api.services.review.rating.dto.request.CreateRatingRequest;
import ams.asmtravel.api.services.review.rating.dto.request.UpdateRatingRequest;
import ams.asmtravel.api.services.review.rating.dto.response.RatingResponse;
import ams.asmtravel.api.services.review.rating.dto.response.RatingSummaryResponse;
import ams.asmtravel.api.services.review.rating.exception.InvalidRatingException;
import ams.asmtravel.api.services.review.rating.exception.RatingAlreadyExistsException;
import ams.asmtravel.api.services.review.rating.exception.RatingNotFoundException;
import ams.asmtravel.api.services.review.rating.mapper.RatingMapper;
import ams.asmtravel.api.services.review.rating.repository.RatingRepository;
import ams.asmtravel.api.services.review.rating.service.RatingService;
import ams.asmtravel.api.services.review.rating.specification.RatingSpecification;
import ams.asmtravel.api.services.point.domain.PointSourceType;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final RatingMapper mapper;
    private final PointService pointService;
    private final DestinationService destinationService;
    private final StayService stayService;
    private final FoodService foodService;
    private final CorridorService corridorService;

    @Override
    @Transactional(readOnly = true)
    public Page<RatingResponse> getAll(RatingTargetType targetType, UUID targetId, Integer stars, Pageable pageable) {
        if (stars != null && (stars < 1 || stars > 5)) {
            throw new InvalidRatingException("Filter by one to five stars");
        }
        return ratingRepository.findAll(RatingSpecification.of(targetType, targetId, null, stars), pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingSummaryResponse> summaries(RatingTargetType targetType, List<UUID> targetIds) {
        if (targetType == null || targetIds == null || targetIds.isEmpty() || targetIds.size() > 100) {
            throw new InvalidRatingException("Name a targetType and 1 to 100 targetIds");
        }
        return summariesOf(targetType, new LinkedHashSet<>(targetIds));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingSummaryResponse> summariesBetween(RatingTargetType targetType, List<UUID> targetIds, Instant from, Instant to) {
        if (targetType == null || targetIds == null || targetIds.isEmpty() || targetIds.size() > 100) {
            throw new InvalidRatingException("Name a targetType and 1 to 100 targetIds");
        }
        Set<UUID> ids = new LinkedHashSet<>(targetIds);
        return summariesOf(targetType, ids, ratingRepository.countStarsBetween(targetType, ids, from, to));
    }

    @Override
    @Transactional(readOnly = true)
    public RatingResponse findById(UUID id) {
        return mapper.toResponse(findActive(id));
    }

    @Override
    @Transactional(readOnly = true)
    public RatingResponse findMine(UUID userId, RatingTargetType targetType, UUID targetId) {
        return ratingRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .filter(rating -> rating.getStatus() == RatingStatus.ACTIVE)
                .map(mapper::toResponse)
                .orElseThrow(() -> new RatingNotFoundException("You have not rated this place"));
    }

    @Override
    @Transactional
    public RatingResponse create(UUID userId, CreateRatingRequest request) {
        Optional<Rating> existing = ratingRepository.findByUserIdAndTargetTypeAndTargetId(
                userId, request.targetType(), request.targetId());
        if (existing.filter(rating -> rating.getStatus() == RatingStatus.ACTIVE).isPresent()) {
            throw new RatingAlreadyExistsException();
        }
        return mapper.toResponse(save(userId, request.targetType(), request.targetId(), request.stars(), existing));
    }

    @Override
    @Transactional
    public RatingResponse update(UUID id, UpdateRatingRequest request, UUID callerId) {
        Rating rating = findActive(id);
        if (!Objects.equals(rating.getUserId(), callerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only its author changes a rating");
        }
        rating.setStars(stars(request.stars()));
        refreshFood(rating.getTargetType(), rating.getTargetId());
        return mapper.toResponse(rating);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID callerId) {
        Rating rating = findActive(id);
        boolean moderator = Authorities.hasAny(Authorities.current(), Permissions.REVIEW_MODERATE, Permissions.FULL_CONTROL);
        if (!Objects.equals(rating.getUserId(), callerId) && !moderator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only its author or a moderator deletes a rating");
        }
        rating.setStatus(RatingStatus.DELETED);
        refreshFood(rating.getTargetType(), rating.getTargetId());
    }

    // For the review service.

    @Override
    @Transactional
    public void rate(UUID userId, RatingTargetType targetType, UUID targetId, int stars) {
        Optional<Rating> existing = ratingRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
        Optional<Rating> active = existing.filter(rating -> rating.getStatus() == RatingStatus.ACTIVE);
        if (active.isPresent()) {
            active.get().setStars(stars(stars));
            refreshFood(targetType, targetId);
        } else {
            save(userId, targetType, targetId, stars, existing);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<RatingService.Rater, Integer> findStars(RatingTargetType targetType, Collection<RatingService.Rater> raters) {
        if (raters.isEmpty()) {
            return Map.of();
        }
        Set<UUID> targetIds = raters.stream().map(RatingService.Rater::targetId).collect(Collectors.toSet());
        Set<RatingService.Rater> wanted = Set.copyOf(raters);
        return ratingRepository.findActive(targetType, targetIds).stream()
                .map(rating -> Map.entry(new RatingService.Rater(rating.getUserId(), rating.getTargetId()), rating.getStars().intValue()))
                .filter(entry -> wanted.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal averageStars() {
        Double average = ratingRepository.averageStars();
        return average == null ? null : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
    }

    // A person's or a place's ratings go with them.

    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        Set<UUID> foods = ratingRepository.findByUserId(event.userId()).stream()
                .filter(rating -> rating.getTargetType() == RatingTargetType.FOOD)
                .map(Rating::getTargetId)
                .collect(Collectors.toSet());
        ratingRepository.deleteByUser(event.userId());
        foods.forEach(food -> refreshFood(RatingTargetType.FOOD, food));
    }

    @EventListener
    public void onDestinationDeleting(DestinationDeletingEvent event) {
        ratingRepository.deleteByTarget(RatingTargetType.DESTINATION, event.destinationId());
    }

    @EventListener
    public void onStayDeleting(StayDeletingEvent event) {
        ratingRepository.deleteByTarget(RatingTargetType.STAY, event.stayId());
    }

    @EventListener
    public void onFoodDeleting(FoodDeletingEvent event) {
        ratingRepository.deleteByTarget(RatingTargetType.FOOD, event.foodId());
    }

    @EventListener
    public void onCorridorDeleting(CorridorDeletingEvent event) {
        ratingRepository.deleteByTarget(RatingTargetType.CORRIDOR, event.corridorId());
    }

    /** A new rating, or a deleted one brought back; either earns the day's point for the place when still due. */
    private Rating save(UUID userId, RatingTargetType targetType, UUID targetId, int stars, Optional<Rating> deleted) {
        if (!isPublished(targetType, targetId)) {
            throw new RatingNotFoundException("Nothing published to rate there");
        }
        Rating rating = deleted.orElseGet(() -> new Rating(userId, targetType, targetId, stars));
        rating.setStars(stars(stars));
        rating.setStatus(RatingStatus.ACTIVE);
        rating = ratingRepository.saveAndFlush(rating);
        pointService.awardDailyTargetPoint(userId, targetType.name(), targetId, PointSourceType.RATING, rating.getId());
        refreshFood(targetType, targetId);
        return rating;
    }

    /** A food listing keeps its average on its own row, so it is refreshed on every change. */
    private void refreshFood(RatingTargetType targetType, UUID targetId) {
        if (targetType == RatingTargetType.FOOD) {
            ratingRepository.flush();
            RatingSummaryResponse summary = summariesOf(RatingTargetType.FOOD, List.of(targetId)).getFirst();
            foodService.updateRating(targetId, summary.average(), (int) summary.count());
        }
    }

    /** One summary per place, in the order asked, unrated ones included; counted by the database. */
    private List<RatingSummaryResponse> summariesOf(RatingTargetType targetType, Collection<UUID> targetIds) {
        return summariesOf(targetType, targetIds, ratingRepository.countStars(targetType, targetIds));
    }

    private List<RatingSummaryResponse> summariesOf(RatingTargetType targetType, Collection<UUID> targetIds,
                                                    List<RatingRepository.StarCount> counts) {
        Map<UUID, Map<Integer, Long>> stars = new HashMap<>();
        for (RatingRepository.StarCount count : counts) {
            stars.computeIfAbsent(count.getTargetId(), id -> new HashMap<>()).put(count.getStars().intValue(), count.getCount());
        }
        return targetIds.stream()
                .map(id -> mapper.toSummaryResponse(targetType, id, stars.getOrDefault(id, Map.of())))
                .toList();
    }

    private boolean isPublished(RatingTargetType targetType, UUID targetId) {
        return switch (targetType) {
            case DESTINATION -> destinationService.isPublished(targetId);
            case STAY -> stayService.isPublished(targetId);
            case FOOD -> foodService.isPublished(targetId);
            case CORRIDOR -> corridorService.isPublished(targetId);
        };
    }

    private static short stars(Integer stars) {
        if (stars == null || stars < 1 || stars > 5) {
            throw new InvalidRatingException("A rating is one to five stars");
        }
        return stars.shortValue();
    }

    private Rating findActive(UUID id) {
        return ratingRepository.findById(id)
                .filter(rating -> rating.getStatus() == RatingStatus.ACTIVE)
                .orElseThrow(RatingNotFoundException::new);
    }
}
