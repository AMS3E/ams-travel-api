package ams.asmtravel.api.services.activity.stamp.repository;

import ams.asmtravel.api.services.activity.stamp.domain.DigitalStamp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StampRepository extends JpaRepository<DigitalStamp, UUID> {

    /** Whether this place was stamped in the window: the season's, for one stamp per place per season. */
    boolean existsByUserIdAndDestinationIdAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
            UUID userId, UUID destinationId, Instant from, Instant to);

    /** Stamps collected in a window: what the Adventure badge counts. */
    long countByUserIdAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(UUID userId, Instant from, Instant to);

    /** How many of these places were stamped in a window: what a collection badge counts. */
    @Query("""
            select count(distinct s.destinationId) from DigitalStamp s
            where s.userId = :userId and s.destinationId in :destinationIds
              and s.collectedAt >= :from and s.collectedAt < :to
            """)
    long countPlacesStamped(UUID userId, Collection<UUID> destinationIds, Instant from, Instant to);

    /** A person's stamps, newest first. */
    List<DigitalStamp> findByUserIdOrderByCollectedAtDesc(UUID userId);

    @Modifying
    @Query("delete from DigitalStamp s where s.userId = :userId")
    int deleteByUserId(UUID userId);
}
