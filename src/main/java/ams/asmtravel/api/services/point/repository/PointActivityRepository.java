package ams.asmtravel.api.services.point.repository;

import ams.asmtravel.api.services.point.domain.PointActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface PointActivityRepository extends JpaRepository<PointActivity, UUID> {

    /** 1 when this is the person's first time with that place this season, 0 otherwise. */
    @Modifying
    @Query(value = """
            insert into gamification.point_activities (id, user_id, season_id, source_type, target_type, target_id, first_at)
            values (:id, :userId, :seasonId, :sourceType, :targetType, :targetId, :now)
            on conflict do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId, @Param("seasonId") UUID seasonId,
                       @Param("sourceType") String sourceType, @Param("targetType") String targetType,
                       @Param("targetId") UUID targetId, @Param("now") Instant now);

    @Modifying
    @Query("delete from PointActivity a where a.userId = :userId")
    void deleteByUser(@Param("userId") UUID userId);
}
