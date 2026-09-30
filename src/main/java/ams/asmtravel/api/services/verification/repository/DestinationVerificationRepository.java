package ams.asmtravel.api.services.verification.repository;

import ams.asmtravel.api.services.verification.domain.DestinationVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DestinationVerificationRepository extends JpaRepository<DestinationVerification, UUID> {

    /** One destination's decisions, newest first. */
    @Query("""
            select v from DestinationVerification v join fetch v.destination
            where v.destination.id = :destinationId
            order by v.createdAt desc, v.id desc
            """)
    List<DestinationVerification> findHistory(UUID destinationId);

    /** Decisions for these destinations, newest first. */
    @Query("""
            select v from DestinationVerification v join fetch v.destination
            where v.destination.id in :destinationIds
            order by v.createdAt desc, v.id desc
            """)
    List<DestinationVerification> findNewestFirst(Collection<UUID> destinationIds);

    /** Every decision, newest first. */
    @Query("select v from DestinationVerification v join fetch v.destination order by v.createdAt desc, v.id desc")
    List<DestinationVerification> findAllNewestFirst();

    boolean existsByDecidedBy(UUID userId);

    @Modifying
    @Query("delete from DestinationVerification v where v.destination.id = :destinationId")
    int deleteByDestinationId(UUID destinationId);
}
