package ams.asmtravel.api.services.corridor.repository;

import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.corridor.domain.CorridorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CorridorRepository extends JpaRepository<Corridor, UUID> {

    /** Featured first, then by name. */
    List<Corridor> findAllByOrderByIsFeaturedDescNameAsc();

    List<Corridor> findAllByStatusOrderByIsFeaturedDescNameAsc(CorridorStatus status);

    Optional<Corridor> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    @Modifying
    @Query(value = "DELETE FROM taxonomy.corridor_interests WHERE interest_id = :id", nativeQuery = true)
    int unlinkInterest(@Param("id") UUID id);
}
