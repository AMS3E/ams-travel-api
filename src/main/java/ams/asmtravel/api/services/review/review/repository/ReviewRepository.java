package ams.asmtravel.api.services.review.review.repository;

import ams.asmtravel.api.services.review.review.domain.Review;
import ams.asmtravel.api.services.review.review.domain.ReviewStatus;
import ams.asmtravel.api.services.review.review.domain.ReviewTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID>, JpaSpecificationExecutor<Review> {

    /** A person's review of a place, deleted or not: one row per person and place. */
    Optional<Review> findByUserIdAndTargetTypeAndTargetId(UUID userId, ReviewTargetType targetType, UUID targetId);

    Page<Review> findByUserIdAndStatusNot(UUID userId, ReviewStatus status, Pageable pageable);

    List<Review> findTop5ByStatusNotOrderByCreatedAtDesc(ReviewStatus status);

    List<Review> findByTargetTypeAndTargetId(ReviewTargetType targetType, UUID targetId);

    List<Review> findByUserId(UUID userId);

    long countByStatus(ReviewStatus status);

    /** The seed's reviews were written some time ago: moved back after the insert. */
    @Modifying
    @Query(value = "update engagement.reviews set created_at = :at, updated_at = :at where id = :id", nativeQuery = true)
    void backdate(@Param("id") UUID id, @Param("at") Instant at);

    long countByStatusNot(ReviewStatus status);

    long countByStatusNotAndCreatedAtGreaterThanEqual(ReviewStatus status, Instant since);

    /** A person's published reviews written in a window: what the Reviewer badge counts. */
    long countByUserIdAndStatusAndCreatedAtBetween(UUID userId, ReviewStatus status, Instant from, Instant to);

    /** Published reviews per place: count and newest, in one grouped query. */
    @Query("""
            select r.targetId, count(r), max(r.createdAt) from Review r
            where r.targetType = :targetType and r.targetId in :targetIds
              and r.status = ams.asmtravel.api.services.review.review.domain.ReviewStatus.PUBLISHED
            group by r.targetId
            """)
    List<Object[]> summarize(@Param("targetType") ReviewTargetType targetType, @Param("targetIds") Collection<UUID> targetIds);

    /** The same, for reviews written in a window; {@code to} exclusive. */
    @Query("""
            select r.targetId, count(r), max(r.createdAt) from Review r
            where r.targetType = :targetType and r.targetId in :targetIds
              and r.status = ams.asmtravel.api.services.review.review.domain.ReviewStatus.PUBLISHED
              and r.createdAt >= :from and r.createdAt < :to
            group by r.targetId
            """)
    List<Object[]> summarizeBetween(@Param("targetType") ReviewTargetType targetType, @Param("targetIds") Collection<UUID> targetIds,
                                    @Param("from") Instant from, @Param("to") Instant to);
}
