package ams.asmtravel.api.services.activity.popular.repository;

import ams.asmtravel.api.services.activity.popular.domain.BrowseEvent;
import ams.asmtravel.api.services.activity.popular.domain.BrowseTarget;
import ams.asmtravel.api.services.activity.popular.domain.BrowseTotal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BrowseEventRepository extends JpaRepository<BrowseEvent, Long> {

    /** Different signed-in visitors per place of one kind since a moment, most first. */
    @Query("""
            select new ams.asmtravel.api.services.activity.popular.domain.BrowseTotal(e.targetId, count(distinct e.userId))
            from BrowseEvent e
            where e.userId is not null and e.targetType = :type and e.targetId is not null and e.createdAt >= :since
            group by e.targetId
            order by count(distinct e.userId) desc
            """)
    List<BrowseTotal> totalsSince(BrowseTarget type, Instant since);

    @Query("""
            select new ams.asmtravel.api.services.activity.popular.domain.BrowseTotal(e.targetId, count(distinct e.userId))
            from BrowseEvent e
            where e.userId is not null and e.targetType = :type and e.targetId is not null
              and e.createdAt >= :from and e.createdAt < :to
            group by e.targetId
            order by count(distinct e.userId) desc
            """)
    List<BrowseTotal> totalsBetween(BrowseTarget type, Instant from, Instant to);

    boolean existsByTargetTypeAndTargetIdAndUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            BrowseTarget targetType, UUID targetId, UUID userId, Instant from, Instant to);

    boolean existsByTargetTypeAndTargetIdAndSessionIdAndUserIdIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            BrowseTarget targetType, UUID targetId, String sessionId, Instant from, Instant to);

    @Query("""
            select e from BrowseEvent e
            where e.userId = :userId and e.targetType = :type and e.targetId is not null
              and e.createdAt >= :from and e.createdAt < :to
            """)
    List<BrowseEvent> findByUserBetween(UUID userId, BrowseTarget type, Instant from, Instant to);

    @Query("select distinct e.targetId from BrowseEvent e where e.userId = :userId and e.targetType = :type and e.targetId is not null")
    List<UUID> findBrowsedTargetIds(UUID userId, BrowseTarget type);

    @Modifying
    @Query("update BrowseEvent e set e.userId = null where e.userId = :userId")
    int forgetUser(UUID userId);

    /** The seed's browses happened over the past month: moved back after the insert. */
    @Modifying
    @Query(value = "update engagement.browse_events set created_at = :at where id = :id", nativeQuery = true)
    void backdate(Long id, Instant at);
}
