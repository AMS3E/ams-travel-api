package ams.asmtravel.api.services.history.regioncoverage.repository;

import ams.asmtravel.api.services.history.regioncoverage.domain.RegionCoverage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegionCoverageRepository extends JpaRepository<RegionCoverage, UUID> {

    @EntityGraph(attributePaths = {"region", "destination"})
    List<RegionCoverage> findAllByOrderByRegionNameAscSortOrderAsc();

    @EntityGraph(attributePaths = {"region", "destination"})
    List<RegionCoverage> findByRegionIdOrderBySortOrderAsc(UUID regionId);

    @EntityGraph(attributePaths = {"region", "destination"})
    Optional<RegionCoverage> findWithLinksById(UUID id);
}
