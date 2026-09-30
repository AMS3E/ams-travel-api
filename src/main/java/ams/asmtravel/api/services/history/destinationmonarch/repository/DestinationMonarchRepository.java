package ams.asmtravel.api.services.history.destinationmonarch.repository;

import ams.asmtravel.api.services.history.destinationmonarch.domain.DestinationMonarch;
import ams.asmtravel.api.services.history.destinationmonarch.domain.DestinationMonarchId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DestinationMonarchRepository extends JpaRepository<DestinationMonarch, DestinationMonarchId> {

    @Query("select l from DestinationMonarch l join fetch l.destination where l.id.destinationId in :destinationIds")
    List<DestinationMonarch> findByDestinations(Collection<UUID> destinationIds);

    /** Every tie, with the place it is on, so a caller's view can be filtered to what they may see. */
    @Query("select l from DestinationMonarch l join fetch l.destination d order by d.name")
    List<DestinationMonarch> findAllWithDestination();

    @Modifying
    @Query("delete from DestinationMonarch l where l.id.destinationId = :destinationId")
    int deleteByDestinationId(UUID destinationId);

    @Modifying
    @Query("delete from DestinationMonarch l where l.id.monarchId = :monarchId")
    int deleteByMonarchId(UUID monarchId);
}
