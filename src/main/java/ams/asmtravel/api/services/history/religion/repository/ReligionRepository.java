package ams.asmtravel.api.services.history.religion.repository;

import ams.asmtravel.api.services.history.religion.domain.Religion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReligionRepository extends JpaRepository<Religion, UUID> {

    List<Religion> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
