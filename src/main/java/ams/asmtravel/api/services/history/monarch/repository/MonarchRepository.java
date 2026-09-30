package ams.asmtravel.api.services.history.monarch.repository;

import ams.asmtravel.api.services.history.monarch.domain.Monarch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MonarchRepository extends JpaRepository<Monarch, UUID> {

    /** In order of reign; one with no known reign last. */
    @Query("""
            select m from Monarch m
            order by case when m.reignStartYear is null then 1 else 0 end, m.reignStartYear, m.name
            """)
    List<Monarch> findAllInReignOrder();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
