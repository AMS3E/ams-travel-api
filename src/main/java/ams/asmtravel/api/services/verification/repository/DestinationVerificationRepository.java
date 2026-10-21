package ams.asmtravel.api.services.verification.repository;

import ams.asmtravel.api.services.verification.domain.DestinationVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DestinationVerificationRepository extends JpaRepository<DestinationVerification, UUID> {

    /** One place's decisions, newest first. */
    List<DestinationVerification> findByTargetIdOrderByCreatedAtDescIdDesc(UUID targetId);

    /** Decisions for these places, newest first. */
    List<DestinationVerification> findByTargetIdInOrderByCreatedAtDescIdDesc(Collection<UUID> targetIds);

    /** Every decision, newest first. */
    List<DestinationVerification> findAllByOrderByCreatedAtDescIdDesc();

    boolean existsByDecidedBy(UUID userId);

    @Modifying
    @Query("delete from DestinationVerification v where v.targetId = :targetId")
    int deleteByTargetId(UUID targetId);
}
