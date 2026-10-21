package ams.asmtravel.api.services.review.review.config;

import ams.asmtravel.api.services.admin.domain.CatalogSeedContext;
import ams.asmtravel.api.services.admin.domain.CatalogSeedContributor;
import ams.asmtravel.api.services.admin.domain.SeedJson;
import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.activity.domain.Activity;
import ams.asmtravel.api.services.destination.food.domain.Food;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.stay.domain.Stay;
import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;
import ams.asmtravel.api.services.review.rating.service.RatingService;
import ams.asmtravel.api.services.review.review.domain.Review;
import ams.asmtravel.api.services.review.review.domain.ReviewStatus;
import ams.asmtravel.api.services.review.review.domain.ReviewTargetType;
import ams.asmtravel.api.services.review.review.domain.ReviewVisitType;
import ams.asmtravel.api.services.review.review.repository.ReviewRepository;
import ams.asmtravel.api.services.user.user.domain.User;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seeds the sample travellers' reviews, each with its star rating, dated back over the past months;
 * a food place's cached average follows, as it does when a review is written.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewSeeder implements CatalogSeedContributor {

    private final ReviewRepository reviewRepository;
    private final RatingService ratingService;
    private final FoodService foodService;
    private final ObjectMapper objectMapper;

    @Override
    public int getOrder() {
        return ENGAGEMENT;
    }

    @Override
    public boolean alreadySeeded() {
        return reviewRepository.count() > 0;
    }

    @Override
    public void seed(CatalogSeedContext context) {
        List<JsonNode> rows = SeedJson.rows(objectMapper, ReviewSeeder.class, "reviews.json");
        Instant now = Instant.now();
        Map<UUID, List<Integer>> foodStars = new HashMap<>();
        int written = 0;
        for (JsonNode node : rows) {
            User traveller = context.require(User.class, SeedJson.text(node, "traveller"), "traveller");
            ReviewTargetType type = SeedJson.enumValue(node, "targetType", ReviewTargetType.class);
            UUID targetId = targetOf(context, type, SeedJson.text(node, "target"));
            if (targetId == null) {
                log.warn("Review of unknown {} {}", type, SeedJson.text(node, "target"));
                continue;
            }
            Review review = new Review();
            review.setUserId(traveller.getId());
            review.setTargetType(type);
            review.setTargetId(targetId);
            review.setTitle(SeedJson.text(node, "title"));
            review.setContent(SeedJson.text(node, "content"));
            review.setStatus(ReviewStatus.PUBLISHED);
            review.setVisitType(SeedJson.enumValue(node, "visitType", ReviewVisitType.class));
            String visited = SeedJson.text(node, "visitedOn");
            review.setVisitedOn(visited == null ? null : YearMonth.parse(visited).atDay(1));
            review = reviewRepository.saveAndFlush(review);
            Integer daysAgo = SeedJson.integer(node, "daysAgo");
            if (daysAgo != null) {
                reviewRepository.backdate(review.getId(), now.minus(Duration.ofDays(daysAgo)));
            }
            int stars = SeedJson.integer(node, "rating");
            ratingService.rate(traveller.getId(), RatingTargetType.valueOf(type.name()), targetId, stars);
            if (type == ReviewTargetType.FOOD) {
                foodStars.computeIfAbsent(targetId, id -> new ArrayList<>()).add(stars);
            }
            written += 1;
        }
        foodStars.forEach((foodId, stars) -> foodService.updateRating(foodId,
                BigDecimal.valueOf(stars.stream().mapToInt(Integer::intValue).sum())
                        .divide(BigDecimal.valueOf(stars.size()), 1, RoundingMode.HALF_UP),
                stars.size()));
        log.info("Seeded {} reviews with their ratings", written);
    }

    private static UUID targetOf(CatalogSeedContext context, ReviewTargetType type, String slug) {
        return switch (type) {
            case DESTINATION -> {
                Destination place = context.find(Destination.class, slug);
                yield place == null ? null : place.getId();
            }
            case STAY -> {
                Stay stay = context.find(Stay.class, slug);
                yield stay == null ? null : stay.getId();
            }
            case FOOD -> {
                Food food = context.find(Food.class, slug);
                yield food == null ? null : food.getId();
            }
            case ACTIVITY -> {
                Activity activity = context.find(Activity.class, slug);
                yield activity == null ? null : activity.getId();
            }
            case CORRIDOR -> {
                Corridor corridor = context.find(Corridor.class, slug);
                yield corridor == null ? null : corridor.getId();
            }
        };
    }

    @Override
    public void wipe() {
        reviewRepository.deleteAllInBatch();
        ratingService.deleteAll();
    }
}
