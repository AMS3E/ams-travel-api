package ams.asmtravel.api.services.corridor.repository;

import ams.asmtravel.api.services.corridor.domain.CorridorDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CorridorDayRepository extends JpaRepository<CorridorDay, UUID> {

    @Query("select d from CorridorDay d where d.corridor.id = :corridorId order by d.dayNumber")
    List<CorridorDay> findByCorridorId(UUID corridorId);

    /** All days, by corridor then day number. */
    @Query("select d from CorridorDay d order by d.corridor.id, d.dayNumber")
    List<CorridorDay> findAllInOrder();
}
