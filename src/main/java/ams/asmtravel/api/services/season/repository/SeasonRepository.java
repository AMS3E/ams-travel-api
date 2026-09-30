package ams.asmtravel.api.services.season.repository;

import ams.asmtravel.api.services.season.domain.Season;
import ams.asmtravel.api.services.season.domain.SeasonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SeasonRepository extends JpaRepository<Season, UUID> {

    List<Season> findAllByOrderByStartsOnDesc();

    /** Active seasons covering a date, latest start first. */
    @Query("""
            select s from Season s
            where s.isActive = true and s.startsOn <= :on and s.endsOn >= :on
            order by s.startsOn desc
            """)
    List<Season> findCovering(@Param("on") LocalDate on);

    /** Active seasons overlapping the range; the caller skips the season being edited. */
    @Query("""
            select s from Season s
            where s.isActive = true and s.startsOn <= :endsOn and s.endsOn >= :startsOn
            """)
    List<Season> findOverlapping(@Param("startsOn") LocalDate startsOn, @Param("endsOn") LocalDate endsOn);

    List<Season> findByStatus(SeasonStatus status);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    boolean existsByStartsOn(LocalDate startsOn);

    boolean existsByStartsOnAndIdNot(LocalDate startsOn, UUID id);
}
