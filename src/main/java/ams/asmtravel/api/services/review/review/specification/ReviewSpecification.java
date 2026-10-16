package ams.asmtravel.api.services.review.review.specification;

import ams.asmtravel.api.services.review.rating.domain.Rating;
import ams.asmtravel.api.services.review.rating.domain.RatingStatus;
import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;
import ams.asmtravel.api.services.review.review.domain.Review;
import ams.asmtravel.api.services.review.review.domain.ReviewStatus;
import ams.asmtravel.api.services.review.review.domain.ReviewTargetType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class ReviewSpecification {

    private ReviewSpecification() {
    }

    public static Specification<Review> of(ReviewTargetType targetType, UUID targetId, ReviewStatus status, UUID userId,
                                           Collection<Integer> stars) {
        return Specification.allOf(
                equal("targetType", targetType),
                equal("targetId", targetId),
                equal("userId", userId),
                status == null ? notDeleted() : equal("status", status),
                ratedWith(stars));
    }

    public static Specification<Review> publishedOf(ReviewTargetType targetType, UUID targetId, Collection<Integer> stars) {
        return Specification.allOf(
                equal("targetType", targetType),
                equal("targetId", targetId),
                equal("status", ReviewStatus.PUBLISHED),
                ratedWith(stars));
    }

    /** A person's own reviews, deleted ones left out. */
    public static Specification<Review> mine(UUID userId, Collection<Integer> stars) {
        return Specification.allOf(equal("userId", userId), notDeleted(), ratedWith(stars));
    }

    public static Specification<Review> notDeleted() {
        return (root, query, builder) -> builder.notEqual(root.get("status"), ReviewStatus.DELETED);
    }

    /**
     * Reviews whose author gave the place one of these star counts. A review carries no stars of its
     * own: the author's rating of the same place is the row beside it, so that is what is looked at.
     */
    public static Specification<Review> ratedWith(Collection<Integer> stars) {
        return (root, query, builder) -> {
            if (stars == null || stars.isEmpty()) {
                return null;
            }
            List<Short> wanted = stars.stream().map(Integer::shortValue).toList();
            List<Predicate> byType = new ArrayList<>();
            for (ReviewTargetType type : ReviewTargetType.values()) {
                RatingTargetType ratingType;
                try {
                    ratingType = RatingTargetType.valueOf(type.name());
                } catch (IllegalArgumentException notRated) {
                    continue;
                }
                Subquery<UUID> rated = query.subquery(UUID.class);
                Root<Rating> rating = rated.from(Rating.class);
                rated.select(rating.get("userId")).where(
                        builder.equal(rating.get("targetType"), ratingType),
                        builder.equal(rating.get("targetId"), root.get("targetId")),
                        builder.equal(rating.get("userId"), root.get("userId")),
                        builder.equal(rating.get("status"), RatingStatus.ACTIVE),
                        rating.get("stars").in(wanted));
                byType.add(builder.and(builder.equal(root.get("targetType"), type), builder.exists(rated)));
            }
            return builder.or(byType.toArray(Predicate[]::new));
        };
    }

    private static Specification<Review> equal(String field, Object value) {
        return (root, query, builder) -> value == null ? null : builder.equal(root.get(field), value);
    }
}
