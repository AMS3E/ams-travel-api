package ams.asmtravel.api.services.taxonomy.interestgroup.repository;

import ams.asmtravel.api.services.taxonomy.interestgroup.domain.InterestGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterestGroupRepository extends JpaRepository<InterestGroup, UUID> {

    List<InterestGroup> findAllByOrderBySortOrderAscNameAsc();
}
