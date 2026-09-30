package ams.asmtravel.api.services.authorization.role.repository;

import ams.asmtravel.api.services.authorization.role.domain.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findByCodeIn(Collection<String> codes);

    @EntityGraph(attributePaths = "permissions")
    List<Role> findAllByOrderByCreatedAtAsc();

    @EntityGraph(attributePaths = "permissions")
    List<Role> findByCodeIgnoreCase(String code);
}
