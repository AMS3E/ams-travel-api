package ams.asmtravel.api.services.history.history.repository;

import ams.asmtravel.api.services.history.history.domain.DestinationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DestinationHistoryRepository extends JpaRepository<DestinationHistory, UUID> {

    /** These places' timelines, each in date order; a moment with no year last. */
    @Query("""
            select h from DestinationHistory h join fetch h.destination
            where h.destination.id in :destinationIds
            order by case when h.eventYear is null then 1 else 0 end, h.eventYear, h.startDate, h.title
            """)
    List<DestinationHistory> findTimelines(Collection<UUID> destinationIds);

    /** Every entry, by place name then date. */
    @Query("""
            select h from DestinationHistory h join fetch h.destination d
            order by d.name, case when h.eventYear is null then 1 else 0 end, h.eventYear, h.startDate, h.title
            """)
    List<DestinationHistory> findAllInOrder();

    @Modifying
    @Query("delete from DestinationHistory h where h.destination.id = :destinationId")
    int deleteByDestinationId(UUID destinationId);

    /** A deleted period's moments stay, in no period. */
    @Modifying
    @Query("update DestinationHistory h set h.historicalPeriodId = null where h.historicalPeriodId = :periodId")
    int clearPeriod(UUID periodId);
}
