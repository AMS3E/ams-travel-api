package ams.asmtravel.api.services.publishing.repository;

import ams.asmtravel.api.services.publishing.domain.PublishingRequest;
import ams.asmtravel.api.services.publishing.domain.PublishingStatus;
import ams.asmtravel.api.services.publishing.domain.PublishingTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublishingRepository extends JpaRepository<PublishingRequest, UUID> {

    /** A listing's requests, newest first: its publishing history. */
    List<PublishingRequest> findByTargetTypeAndTargetIdOrderByRequestedAtDesc(PublishingTargetType targetType, UUID targetId);

    Optional<PublishingRequest> findFirstByTargetTypeAndTargetIdOrderByRequestedAtDesc(PublishingTargetType targetType, UUID targetId);

    List<PublishingRequest> findByStatusInOrderByRequestedAtDesc(Collection<PublishingStatus> statuses);

    List<PublishingRequest> findByRequestedByOrderByRequestedAtDesc(UUID requestedBy);

    /** Whether this person asked for, or answered, any request: history that has to outlive them. */
    @Query("select count(r) > 0 from PublishingRequest r where r.requestedBy = :userId or r.reviewerId = :userId")
    boolean involves(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from PublishingRequest r where r.targetType = :targetType and r.targetId = :targetId")
    void deleteByTarget(@Param("targetType") PublishingTargetType targetType, @Param("targetId") UUID targetId);

    long countByStatus(PublishingStatus status);
}
