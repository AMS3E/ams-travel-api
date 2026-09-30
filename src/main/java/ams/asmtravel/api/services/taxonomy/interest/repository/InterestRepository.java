package ams.asmtravel.api.services.taxonomy.interest.repository;

import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterestRepository extends JpaRepository<Interest, UUID> {

    List<Interest> findAllByOrderByNameAsc();

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);
}
