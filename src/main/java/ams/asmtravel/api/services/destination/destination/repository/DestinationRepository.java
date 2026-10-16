package ams.asmtravel.api.services.destination.destination.repository;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DestinationRepository extends JpaRepository<Destination, UUID>, JpaSpecificationExecutor<Destination> {
    /** Up to a page of published places whose name contains the text, A to Z: the search box's suggestions. */
    List<Destination> findByStatusAndNameContainingIgnoreCaseOrderByNameAsc(ListingStatus status, String name, Pageable pageable);


    /* Joins province and zone; collections load in batches. */
    @EntityGraph(attributePaths = {"province", "province.region"})
    List<Destination> findAllByStatusOrderByNameAsc(ListingStatus status);

    /* Joins province and zone; collections load in batches. */
    @EntityGraph(attributePaths = {"province", "province.region"})
    List<Destination> findAllByOrderByNameAsc();

    /* Joins province and zone; collections load in batches. */
    @EntityGraph(attributePaths = {"province", "province.region"})
    List<Destination> findAllByCreatedByInOrderByNameAsc(Collection<UUID> createdBy);

    @EntityGraph(attributePaths = {"province", "province.region", "categories", "tags", "interests",
            "tourismRegions"})
    Optional<Destination> findWithLinksById(UUID id);

    @EntityGraph(attributePaths = {"province", "province.region", "categories", "tags", "interests",
            "tourismRegions"})
    Optional<Destination> findBySlugAndStatus(String slug, ListingStatus status);

    @EntityGraph(attributePaths = {"province", "province.region", "categories", "interests", "tourismRegions"})
    List<Destination> findAllByIdIn(Collection<UUID> ids);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    boolean existsByProvinceId(UUID provinceId);

    long countByStatus(ListingStatus status);

    /** Bulk update on purpose: a visit writes no audit row and no {@code updated_at}. */
    @Modifying
    @Query("update Destination d set d.viewCount = d.viewCount + 1 where d.id = :id")
    int incrementViewCount(UUID id);

    /** PostgreSQL only; must match {@code idx_destinations_location} (expression and WHERE) to use the index. */
    @Query(value = """
            SELECT d.id
            FROM destination.destinations d
            WHERE d.status = 'PUBLISHED'
              AND d.latitude IS NOT NULL AND d.longitude IS NOT NULL
              AND ST_DWithin(
                    ST_SetSRID(ST_MakePoint(d.longitude::float8, d.latitude::float8), 4326)::geography,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :radiusMeters)
            ORDER BY ST_Distance(
                    ST_SetSRID(ST_MakePoint(d.longitude::float8, d.latitude::float8), 4326)::geography,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography)
            LIMIT :resultLimit
            """, nativeQuery = true)
    List<UUID> findNearbyIds(@Param("latitude") double latitude,
                             @Param("longitude") double longitude,
                             @Param("radiusMeters") double radiusMeters,
                             @Param("resultLimit") int resultLimit);

    /** One row of {@link #findNearbyDistances}: a place and how far it is from the point, in metres. */
    interface NearbyDistance {
        UUID getId();

        Double getMetres();
    }

    /** {@link #findNearbyIds} with each distance, measured the same way as the radius. PostgreSQL only. */
    @Query(value = """
            SELECT d.id AS id,
                   ST_Distance(
                     ST_SetSRID(ST_MakePoint(d.longitude::float8, d.latitude::float8), 4326)::geography,
                     ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) AS metres
            FROM destination.destinations d
            WHERE d.status = 'PUBLISHED'
              AND d.latitude IS NOT NULL AND d.longitude IS NOT NULL
              AND ST_DWithin(
                    ST_SetSRID(ST_MakePoint(d.longitude::float8, d.latitude::float8), 4326)::geography,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :radiusMeters)
            ORDER BY metres, d.id
            LIMIT :resultLimit
            """, nativeQuery = true)
    List<NearbyDistance> findNearbyDistances(@Param("latitude") double latitude,
                                             @Param("longitude") double longitude,
                                             @Param("radiusMeters") double radiusMeters,
                                             @Param("resultLimit") int resultLimit);

    /** Unlinks a deleted category, tag, interest or region. */
    @Modifying
    @Query(value = "DELETE FROM destination.destination_categories WHERE category_id = :id", nativeQuery = true)
    int unlinkCategory(@Param("id") UUID id);

    @Modifying
    @Query(value = "DELETE FROM destination.destination_tags WHERE tag_id = :id", nativeQuery = true)
    int unlinkTag(@Param("id") UUID id);

    @Modifying
    @Query(value = "DELETE FROM destination.destination_interests WHERE interest_id = :id", nativeQuery = true)
    int unlinkInterest(@Param("id") UUID id);

    @Modifying
    @Query(value = "DELETE FROM destination.destination_regions WHERE region_id = :id", nativeQuery = true)
    int unlinkRegion(@Param("id") UUID id);

}
