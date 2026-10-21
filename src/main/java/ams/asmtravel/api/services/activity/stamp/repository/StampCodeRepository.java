package ams.asmtravel.api.services.activity.stamp.repository;

import ams.asmtravel.api.services.activity.stamp.domain.StampCode;
import ams.asmtravel.api.services.activity.stamp.domain.StampTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StampCodeRepository extends JpaRepository<StampCode, UUID> {

    Optional<StampCode> findByCode(String code);

    /** A place's codes, the current one first. */
    List<StampCode> findByTargetTypeAndTargetIdOrderByIssuedAtDesc(StampTarget targetType, UUID targetId);

    Optional<StampCode> findByTargetTypeAndTargetIdAndReplacedAtIsNull(StampTarget targetType, UUID targetId);
}
