package ams.asmtravel.api.services.location.region.repository;

import ams.asmtravel.api.services.location.region.domain.Region;
import ams.asmtravel.api.services.location.region.domain.RegionKind;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegionRepository extends JpaRepository<Region, UUID> {

    @EntityGraph(attributePaths = {"categories", "tags", "interests"})
    List<Region> findAllByOrderByNameAsc();

    /** The Discover layer asks for one kind at a time: the zones, or the themes. */
    @EntityGraph(attributePaths = {"categories", "tags", "interests"})
    List<Region> findAllByKindOrderByNameAsc(RegionKind kind);

    @EntityGraph(attributePaths = {"categories", "tags", "interests"})
    Optional<Region> findWithLinksById(UUID id);

    /** The regions of one kind, for numbering them: their links are not needed. */
    List<Region> findAllByKind(RegionKind kind);

    /** The regions whose shelves hold this category, or whose filters hold this tag, with their lists. */
    @EntityGraph(attributePaths = {"categories", "tags"})
    List<Region> findDistinctByCategories_Id(UUID categoryId);

    @EntityGraph(attributePaths = {"categories", "tags"})
    List<Region> findDistinctByTags_Id(UUID tagId);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByKindAndCodeIgnoreCase(RegionKind kind, String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    /** A deleted zone's children become top level. */
    @Modifying
    @Query("update Region r set r.parentId = null where r.parentId = :parentId")
    int detachChildren(UUID parentId);
}
