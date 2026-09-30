package ams.asmtravel.api.services.point.repository;

import ams.asmtravel.api.services.point.domain.PointTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {

    /** Writes the day's point once; returns 1 when written, 0 when it already existed. */
    @Modifying
    @Query(value = """
            insert into gamification.point_transactions
              (id, user_id, target_type, target_id, source_type, source_id, season_id, points, reward_date, created_at)
            values (:id, :userId, :targetType, :targetId, :sourceType, :sourceId, :seasonId, :points, :rewardDate, :now)
            on conflict do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId, @Param("targetType") String targetType,
                       @Param("targetId") UUID targetId, @Param("sourceType") String sourceType,
                       @Param("sourceId") UUID sourceId, @Param("seasonId") UUID seasonId, @Param("points") int points,
                       @Param("rewardDate") LocalDate rewardDate, @Param("now") Instant now);

    @Query("""
            select coalesce(sum(p.points), 0) from PointTransaction p
            where p.userId = :userId and p.rewardDate between :from and :to
            """)
    long sumPoints(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    Page<PointTransaction> findByUserId(UUID userId, Pageable pageable);

    Page<PointTransaction> findByUserIdAndSeasonId(UUID userId, UUID seasonId, Pageable pageable);

    @Modifying
    @Query("delete from PointTransaction p where p.userId = :userId")
    void deleteByUser(@Param("userId") UUID userId);
}
