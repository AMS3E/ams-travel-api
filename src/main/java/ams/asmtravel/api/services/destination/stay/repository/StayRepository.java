package ams.asmtravel.api.services.destination.stay.repository;

import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.stay.domain.Stay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface StayRepository extends JpaRepository<Stay, UUID> {

    List<Stay> findAllByOrderByNameAsc();

    List<Stay> findByStatusOrderByNameAsc(ListingStatus status);

    List<Stay> findByDestinationIdOrderByNameAsc(UUID destinationId);

    List<Stay> findByDestinationIdAndStatusOrderByNameAsc(UUID destinationId, ListingStatus status);

    /** Stays inside a lat/long box; the service measures the real distance. */
    List<Stay> findByStatusAndLatitudeBetweenAndLongitudeBetween(ListingStatus status,
                                                                 BigDecimal south, BigDecimal north,
                                                                 BigDecimal west, BigDecimal east);

    @Modifying
    @Query(value = "DELETE FROM destination.destination_stay_interests WHERE interest_id = :id", nativeQuery = true)
    int unlinkInterest(@Param("id") UUID id);

    /** Unlinks a deleted destination. */
    @Modifying
    @Query("update Stay s set s.destination = null where s.destination.id = :destinationId")
    int releaseDestination(UUID destinationId);

    long countByStatus(ListingStatus status);

    /** How many stays list an amenity slug; the needle is the quoted slug between wildcards. The JSON is read as text, which both databases allow. */
    @Query(value = "SELECT count(*) FROM destination.destination_stays WHERE CAST(amenities AS varchar) LIKE :needle", nativeQuery = true)
    long countByAmenity(@Param("needle") String needle);

    /** A deleted owner's stays stay listed with no owner. */
    @Modifying
    @Query("update Stay s set s.ownerUserId = null where s.ownerUserId = :ownerUserId")
    int releaseOwner(UUID ownerUserId);
}
