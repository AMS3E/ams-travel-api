package ams.asmtravel.api.services.destination.food.repository;

import ams.asmtravel.api.services.destination.food.domain.Food;
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

public interface FoodRepository extends JpaRepository<Food, UUID> {

    @EntityGraph(attributePaths = {"categories", "tags", "interests"})
    List<Food> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"categories", "tags", "interests"})
    List<Food> findByStatusOrderByNameAsc(ListingStatus status);

    @EntityGraph(attributePaths = {"categories", "tags", "interests"})
    List<Food> findByDestinationIdOrderByNameAsc(UUID destinationId);

    @EntityGraph(attributePaths = {"categories", "tags", "interests"})
    List<Food> findByDestinationIdAndStatusOrderByNameAsc(UUID destinationId, ListingStatus status);

    /** Listings inside a lat/long box; the service measures the real distance. */
    @EntityGraph(attributePaths = {"categories", "tags", "interests"})
    List<Food> findByStatusAndLatitudeBetweenAndLongitudeBetween(ListingStatus status,
                                                                 BigDecimal south, BigDecimal north,
                                                                 BigDecimal west, BigDecimal east);

    @EntityGraph(attributePaths = {"categories", "tags", "interests"})
    Optional<Food> findWithLinksById(UUID id);

    @EntityGraph(attributePaths = {"categories", "tags", "interests"})
    Optional<Food> findBySlug(String slug);


    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, UUID id);

    long countByStatus(ListingStatus status);

    /** Bulk update of the rating: not counted as an edit. */
    @Modifying
    @Query("update Food f set f.averageRating = :average, f.reviewCount = :count where f.id = :id")
    int updateRating(UUID id, BigDecimal average, int count);

    @Modifying
    @Query("update Food f set f.ownerUserId = null where f.ownerUserId = :ownerUserId")
    int releaseOwner(UUID ownerUserId);

    /** Unlinks a deleted destination. */
    @Modifying
    @Query("update Food f set f.destination = null where f.destination.id = :destinationId")
    int releaseDestination(UUID destinationId);

    @Modifying
    @Query(value = "DELETE FROM destination.destination_food_categories WHERE category_id = :id", nativeQuery = true)
    int unlinkCategory(@Param("id") UUID id);

    @Modifying
    @Query(value = "DELETE FROM destination.destination_food_tags WHERE tag_id = :id", nativeQuery = true)
    int unlinkTag(@Param("id") UUID id);

    @Modifying
    @Query(value = "DELETE FROM destination.destination_food_interests WHERE interest_id = :id", nativeQuery = true)
    int unlinkInterest(@Param("id") UUID id);
}
