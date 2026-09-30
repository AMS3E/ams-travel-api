package ams.asmtravel.api.services.season.repository;

import ams.asmtravel.api.services.season.domain.UserSeasonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSeasonProgressRepository extends JpaRepository<UserSeasonProgress, UUID> {

    Optional<UserSeasonProgress> findByUserIdAndSeasonId(UUID userId, UUID seasonId);

    List<UserSeasonProgress> findByUserIdOrderByStartedAtDesc(UUID userId);

    /** Starts a person's row for a season; a row already there is left alone. */
    @Modifying
    @Query(value = """
            insert into gamification.user_season_progress
              (id, user_id, season_id, points, rating_count, review_count,
               unique_rated_target_count, unique_reviewed_target_count, started_at, updated_at)
            values (:id, :userId, :seasonId, 0, 0, 0, 0, 0, :now, :now)
            on conflict do nothing
            """, nativeQuery = true)
    void insertIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId, @Param("seasonId") UUID seasonId,
                        @Param("now") Instant now);

    /** Adds to the totals in one statement, so concurrent activity is never lost. */
    @Modifying
    @Query("""
            update UserSeasonProgress p set
              p.points = p.points + :points,
              p.ratingCount = p.ratingCount + :ratings,
              p.reviewCount = p.reviewCount + :reviews,
              p.uniqueRatedTargetCount = p.uniqueRatedTargetCount + :newRated,
              p.uniqueReviewedTargetCount = p.uniqueReviewedTargetCount + :newReviewed,
              p.updatedAt = :now
            where p.userId = :userId and p.seasonId = :seasonId
            """)
    void add(@Param("userId") UUID userId, @Param("seasonId") UUID seasonId, @Param("points") int points,
             @Param("ratings") int ratings, @Param("reviews") int reviews,
             @Param("newRated") int newRated, @Param("newReviewed") int newReviewed, @Param("now") Instant now);

    @Modifying
    @Query("delete from UserSeasonProgress p where p.userId = :userId")
    void deleteByUser(@Param("userId") UUID userId);
}
