package ams.asmtravel.api.services.history.architecture.repository;

import ams.asmtravel.api.services.history.architecture.domain.ArchitectureStyle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ArchitectureStyleRepository extends JpaRepository<ArchitectureStyle, UUID> {

    List<ArchitectureStyle> findAllByOrderByNameAsc();

    List<ArchitectureStyle> findAllByPeriodIdOrderByNameAsc(UUID periodId);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    /** A deleted period's styles stay, in no period. */
    @Modifying
    @Query("update ArchitectureStyle s set s.periodId = null where s.periodId = :periodId")
    int clearPeriod(UUID periodId);
}
