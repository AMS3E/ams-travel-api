package ams.asmtravel.api.services.activity.popular.repository;

import ams.asmtravel.api.services.activity.popular.domain.BrowseEvent;
import ams.asmtravel.api.services.activity.popular.domain.BrowseTotal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface BrowseEventRepository extends JpaRepository<BrowseEvent, Long> {

    /** Different signed-in visitors per place since a moment, most first. */
    @Query("""
            select new ams.asmtravel.api.services.activity.popular.domain.BrowseTotal(e.destinationId, count(distinct e.userId))
            from BrowseEvent e
            where e.userId is not null and e.destinationId is not null and e.createdAt >= :since
            group by e.destinationId
            order by count(distinct e.userId) desc
            """)
    List<BrowseTotal> totalsSince(Instant since);

    /** Whether this visitor already opened this place in the window: the same day. */
    boolean existsByDestinationIdAndUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID destinationId, UUID userId, Instant from, Instant to);

    /** The same for an anonymous visitor, known only by the website's session. */
    boolean existsByDestinationIdAndSessionIdAndUserIdIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID destinationId, String sessionId, Instant from, Instant to);

    /** When a visitor opened which places, in a window: what the Browser badge counts. */
    @Query("""
            select e from BrowseEvent e
            where e.userId = :userId and e.destinationId is not null and e.createdAt >= :from and e.createdAt < :to
            """)
    List<BrowseEvent> findByUserBetween(UUID userId, Instant from, Instant to);

    @Query("select distinct e.destinationId from BrowseEvent e where e.userId = :userId and e.destinationId is not null")
    List<UUID> findBrowsedDestinationIds(UUID userId);

    /** A deleted person's browses stay, with nobody behind them. */
    @Modifying
    @Query("update BrowseEvent e set e.userId = null where e.userId = :userId")
    int forgetUser(UUID userId);
}
