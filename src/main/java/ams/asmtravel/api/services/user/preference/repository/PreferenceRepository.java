package ams.asmtravel.api.services.user.preference.repository;

import ams.asmtravel.api.services.user.preference.domain.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface PreferenceRepository extends JpaRepository<UserPreferences, UUID> {

    /** A deleted region is no one's home region any more. */
    @Modifying
    @Query("update UserPreferences p set p.preferredRegionId = null where p.preferredRegionId = :regionId")
    int clearPreferredRegion(UUID regionId);
}
