package ams.asmtravel.api.services.user.user.repository;

import ams.asmtravel.api.services.user.user.domain.AuthIdentity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityRepository extends JpaRepository<AuthIdentity, UUID> {

    /** The person behind a token: an account id is unique within its realm only. */
    @EntityGraph(attributePaths = "user")
    Optional<AuthIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<AuthIdentity> findFirstByUserIdAndProvider(UUID userId, String provider);

    List<AuthIdentity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AuthIdentity> findAllByOrderByCreatedAtDesc();
}
