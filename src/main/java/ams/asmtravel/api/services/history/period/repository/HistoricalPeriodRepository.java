package ams.asmtravel.api.services.history.period.repository;

import ams.asmtravel.api.services.history.period.domain.HistoricalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface HistoricalPeriodRepository extends JpaRepository<HistoricalPeriod, UUID> {

    /** Oldest first; one with no clear beginning is the oldest. */
    @Query("""
            select p from HistoricalPeriod p
            order by case when p.startYear is null then 0 else 1 end, p.startYear, p.name
            """)
    List<HistoricalPeriod> findAllOldestFirst();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
