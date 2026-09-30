package ams.asmtravel.api.services.location.province.repository;

import ams.asmtravel.api.services.location.province.domain.Province;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProvinceRepository extends JpaRepository<Province, UUID> {

    @EntityGraph(attributePaths = "region")
    List<Province> findAllByOrderByNameAsc();

    boolean existsBySlug(String slug);

    boolean existsByRegionId(UUID regionId);
}
