package ams.asmtravel.api.services.user.user.repository;

import ams.asmtravel.api.services.user.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByKeycloakId(UUID keycloakId);

    List<User> findAllByKeycloakIdIn(Collection<UUID> keycloakIds);

    /** Every person, newest first: the dashboard's list. */
    List<User> findAllByOrderByCreatedAtDesc();

    List<User> findAllByIdIn(Collection<UUID> ids);

    long countByEmailEndingWith(String suffix);

    List<User> findByEmailEndingWith(String suffix);

    /** The seed's travellers joined some time ago: their timestamps are moved back after the insert. */
    @Modifying
    @Query(value = "update auth.users set created_at = :at, updated_at = :at where id = :id", nativeQuery = true)
    void backdate(@Param("id") UUID id, @Param("at") Instant at);
}
