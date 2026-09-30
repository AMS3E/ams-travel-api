package ams.asmtravel.api.services.authorization.role.repository;

import ams.asmtravel.api.services.authorization.role.domain.UserRole;
import ams.asmtravel.api.services.authorization.role.domain.UserRoleId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    @EntityGraph(attributePaths = "role")
    List<UserRole> findByIdUserId(UUID userId);

    @EntityGraph(attributePaths = "role")
    List<UserRole> findByIdUserIdIn(Collection<UUID> userIds);

    @EntityGraph(attributePaths = "role")
    List<UserRole> findAllByOrderByAssignedAtDesc();

    /** Users holding any of these roles. */
    @Query("select distinct ur.id.userId from UserRole ur where ur.role.code in :codes")
    List<UUID> findUserIdsHoldingAny(Collection<String> codes);
}
