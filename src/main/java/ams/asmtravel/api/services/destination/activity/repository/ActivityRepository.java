package ams.asmtravel.api.services.destination.activity.repository;

import ams.asmtravel.api.services.destination.activity.domain.Activity;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {

    @EntityGraph(attributePaths = {"interests"})
    List<Activity> findAllByOrderByNameAsc();

    /** Bulk update on purpose: a visit writes no audit row and no {@code updated_at}. */
    @Modifying
    @Query("update Activity x set x.viewCount = x.viewCount + 1 where x.id = :id")
    int incrementViewCount(UUID id);

    @EntityGraph(attributePaths = {"interests"})
    List<Activity> findByStatusOrderByNameAsc(ListingStatus status);

    @EntityGraph(attributePaths = {"interests"})
    List<Activity> findByDestinationIdOrderByNameAsc(UUID destinationId);

    /** Activities inside a lat/long box; the service measures the real distance. */
    @EntityGraph(attributePaths = {"interests"})
    List<Activity> findByStatusAndLatitudeBetweenAndLongitudeBetween(ListingStatus status,
                                                                     BigDecimal south, BigDecimal north,
                                                                     BigDecimal west, BigDecimal east);

    @EntityGraph(attributePaths = {"interests"})
    Optional<Activity> findWithLinksById(UUID id);

    @EntityGraph(attributePaths = {"interests"})
    Optional<Activity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    long countByStatus(ListingStatus status);

    boolean existsByProvinceId(UUID provinceId);

    @Modifying
    @Query("update Activity a set a.ownerUserId = null where a.ownerUserId = :ownerUserId")
    int releaseOwner(UUID ownerUserId);

    /** Unlinks a deleted attraction. */
    @Modifying
    @Query("update Activity a set a.destination = null where a.destination.id = :destinationId")
    int releaseDestination(UUID destinationId);

    @Modifying
    @Query(value = "DELETE FROM destination.destination_activity_interests WHERE interest_id = :id", nativeQuery = true)
    int unlinkInterest(@Param("id") UUID id);
}
