package ams.asmtravel.api.services.user.user.repository;

import ams.asmtravel.api.services.user.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByKeycloakId(UUID keycloakId);

    /** Every person, newest first: the dashboard's list. */
    List<User> findAllByOrderByCreatedAtDesc();

    List<User> findAllByIdIn(Collection<UUID> ids);
}
