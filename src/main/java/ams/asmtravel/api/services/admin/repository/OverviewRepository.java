package ams.asmtravel.api.services.admin.repository;

import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.destination.food.service.FoodService;
import ams.asmtravel.api.services.destination.stay.service.StayService;
import ams.asmtravel.api.services.review.review.service.ReviewService.ReviewTotals;
import ams.asmtravel.api.services.review.review.dto.response.ReviewResponse;
import ams.asmtravel.api.services.favorite.service.FavoriteService;
import ams.asmtravel.api.services.review.review.service.ReviewService;
import ams.asmtravel.api.services.season.dto.response.SeasonResponse;
import ams.asmtravel.api.services.badge.service.BadgeService;
import ams.asmtravel.api.services.season.service.SeasonService;
import ams.asmtravel.api.services.taxonomy.category.service.CategoryService;
import ams.asmtravel.api.services.corridor.service.CorridorService;
import ams.asmtravel.api.services.taxonomy.interest.service.InterestService;
import ams.asmtravel.api.services.location.province.service.ProvinceService;
import ams.asmtravel.api.services.location.region.service.RegionService;
import ams.asmtravel.api.services.taxonomy.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Dashboard figures, read through each owning service; admin keeps no rows. */
@Repository
@RequiredArgsConstructor
public class OverviewRepository {

    private final DestinationService destinationService;
    private final RegionService regionService;
    private final ProvinceService provinceService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final InterestService interestService;
    private final CorridorService corridorService;
    private final StayService stayService;
    private final FoodService foodService;
    private final UserService userService;
    private final ReviewService reviewService;
    private final FavoriteService favoriteService;
    private final BadgeService badgeService;
    private final SeasonService seasonService;

    /** Rows per table, keyed as the dashboard names them. */
    public Map<String, Long> counts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("destinations", destinationService.count());
        counts.put("regions", regionService.count());
        counts.put("provinces", provinceService.count());
        counts.put("categories", categoryService.count());
        counts.put("tags", tagService.count());
        counts.put("interests", interestService.count());
        counts.put("corridors", corridorService.count());
        counts.put("stays", stayService.count());
        counts.put("foods", foodService.count());
        /* A partner is an account with the PARTNER role, not a table. */
        counts.put("partners", (long) userService.findUserIdsHoldingAny(Set.of(RoleCode.PARTNER)).size());
        counts.put("users", userService.count());
        counts.put("favorites", favoriteService.count());
        counts.put("badges", badgeService.count());
        return counts;
    }

    public Optional<SeasonResponse> currentSeason() {
        return seasonService.findCurrent();
    }

    /** @param season the running season, or null: reviews since it began are counted only then */
    public ReviewTotals reviewTotals(SeasonResponse season) {
        Instant since = season == null ? null : seasonService.windowOf(season.startsOn(), season.endsOn()).from();
        return reviewService.totals(since);
    }

    public List<ReviewResponse> recentReviews(int limit) {
        return reviewService.findRecent().stream().limit(limit).toList();
    }
}
