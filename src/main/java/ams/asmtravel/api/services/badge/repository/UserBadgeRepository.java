package ams.asmtravel.api.services.badge.repository;

import ams.asmtravel.api.services.badge.domain.UserBadge;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {

    Optional<UserBadge> findByUserIdAndBadgeIdAndSeasonId(UUID userId, UUID badgeId, UUID seasonId);

    @EntityGraph(attributePaths = "badge")
    List<UserBadge> findByUserIdAndSeasonIdOrderByUpdatedAtDesc(UUID userId, UUID seasonId);

    @EntityGraph(attributePaths = "badge")
    List<UserBadge> findByUserIdAndEarnedAtIsNotNullOrderByEarnedAtDesc(UUID userId);

    @EntityGraph(attributePaths = "badge")
    List<UserBadge> findByUserIdAndEarnedAtGreaterThanEqualOrderByEarnedAtAsc(UUID userId, Instant since);

    boolean existsByBadgeIdAndEarnedAtIsNotNull(UUID badgeId);

    /** Every member with a row in the season: who, how many badges earned, and their progress added up. */
    @Query("""
            select u.userId, sum(case when u.earnedAt is not null then 1 else 0 end), sum(u.progress)
            from UserBadge u where u.seasonId = :seasonId group by u.userId
            """)
    List<Object[]> activityIn(@Param("seasonId") UUID seasonId);

    @Modifying
    @Query("delete from UserBadge u where u.userId = :userId")
    void deleteByUser(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from UserBadge u where u.badge.id = :badgeId")
    void deleteByBadge(@Param("badgeId") UUID badgeId);
}
