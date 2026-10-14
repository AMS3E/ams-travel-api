package ams.asmtravel.api.services.corridor.repository;

import ams.asmtravel.api.services.corridor.domain.CorridorStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CorridorStopRepository extends JpaRepository<CorridorStop, UUID> {

    /** One corridor's stops in travel order, with their destinations. */
    @Query("""
            select s from CorridorStop s
            join fetch s.day d
            join fetch s.destination place
            join fetch place.province province
            left join fetch province.region
            where d.corridor.id = :corridorId
            order by d.dayNumber, s.stopOrder
            """)
    List<CorridorStop> findInTravelOrder(UUID corridorId);

    /** All stops, by corridor then travel order. */
    @Query("""
            select s from CorridorStop s
            join fetch s.day d
            join fetch s.destination place
            left join fetch place.province province
            left join fetch province.region
            order by d.corridor.id, d.dayNumber, s.stopOrder
            """)
    List<CorridorStop> findAllInTravelOrder();

    @Query("select distinct d.corridor.id from CorridorStop s join s.day d where s.destination.id = :destinationId")
    List<UUID> findCorridorIdsStoppingAt(UUID destinationId);

    @Query("select distinct s.day.id from CorridorStop s where s.destination.id = :destinationId")
    List<UUID> findDayIdsStoppingAt(UUID destinationId);

    @Query("select s from CorridorStop s where s.day.id in :dayIds order by s.day.id, s.stopOrder")
    List<CorridorStop> findByDayIds(List<UUID> dayIds);

    /** Bulk delete, so later stops can be renumbered without a unique-key clash. */
    @Modifying
    @Query("delete from CorridorStop s where s.destination.id = :destinationId")
    int deleteByDestinationId(UUID destinationId);
}
