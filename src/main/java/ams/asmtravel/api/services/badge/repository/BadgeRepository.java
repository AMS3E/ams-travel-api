package ams.asmtravel.api.services.badge.repository;

import ams.asmtravel.api.services.badge.domain.Badge;
import ams.asmtravel.api.services.badge.domain.BadgeStatus;
import ams.asmtravel.api.services.badge.domain.BadgeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface BadgeRepository extends JpaRepository<Badge, UUID> {

    @EntityGraph(attributePaths = "destinationIds")
    List<Badge> findAllByOrderByTypeAscThresholdAsc();

    @EntityGraph(attributePaths = "destinationIds")
    List<Badge> findByStatusAndTypeInOrderByThresholdAsc(BadgeStatus status, Collection<BadgeType> types);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    /** A deleted destination leaves every collection it was in. */
    @Modifying
    @Query(value = "delete from gamification.badge_destinations where destination_id = :destinationId", nativeQuery = true)
    void removeDestination(@Param("destinationId") UUID destinationId);
}
