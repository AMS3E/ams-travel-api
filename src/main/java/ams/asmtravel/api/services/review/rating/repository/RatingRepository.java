package ams.asmtravel.api.services.review.rating.repository;

import ams.asmtravel.api.services.review.rating.domain.Rating;
import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RatingRepository extends JpaRepository<Rating, UUID>, JpaSpecificationExecutor<Rating> {

    /** How many active ratings of one place gave one number of stars. */
    interface StarCount {

        UUID getTargetId();

        Short getStars();

        Long getCount();
    }

    /** A person's rating of a place, deleted or not: one row per person and place. */
    Optional<Rating> findByUserIdAndTargetTypeAndTargetId(UUID userId, RatingTargetType targetType, UUID targetId);

    /** Active ratings of these places, for matching reviews to their authors' stars. */
    @Query("""
            select r from Rating r
            where r.targetType = :targetType and r.targetId in :targetIds
              and r.status = ams.asmtravel.api.services.review.rating.domain.RatingStatus.ACTIVE
            """)
    List<Rating> findActive(@Param("targetType") RatingTargetType targetType, @Param("targetIds") Collection<UUID> targetIds);

    List<Rating> findByUserId(UUID userId);

    /** Each place's active ratings, counted per number of stars: average, total and distribution come from this. */
    @Query("""
            select r.targetId as targetId, r.stars as stars, count(r) as count
            from Rating r
            where r.targetType = :targetType and r.targetId in :targetIds
              and r.status = ams.asmtravel.api.services.review.rating.domain.RatingStatus.ACTIVE
            group by r.targetId, r.stars
            """)
    List<StarCount> countStars(@Param("targetType") RatingTargetType targetType, @Param("targetIds") Collection<UUID> targetIds);

    @Query("select avg(r.stars) from Rating r where r.status = ams.asmtravel.api.services.review.rating.domain.RatingStatus.ACTIVE")
    Double averageStars();

    @Modifying
    @Query("delete from Rating r where r.userId = :userId")
    void deleteByUser(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from Rating r where r.targetType = :targetType and r.targetId = :targetId")
    void deleteByTarget(@Param("targetType") RatingTargetType targetType, @Param("targetId") UUID targetId);
}
