package ams.asmtravel.api.services.review.review.specification;

import ams.asmtravel.api.services.review.review.domain.Review;
import ams.asmtravel.api.services.review.review.domain.ReviewStatus;
import ams.asmtravel.api.services.review.review.domain.ReviewTargetType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/** Filters for listing reviews; a null value leaves that filter out. */
public final class ReviewSpecification {

    private ReviewSpecification() {
    }

    /** What a moderator asked for; deleted reviews only when asked for by status. */
    public static Specification<Review> of(ReviewTargetType targetType, UUID targetId, ReviewStatus status, UUID userId) {
        return Specification.allOf(
                equal("targetType", targetType),
                equal("targetId", targetId),
                equal("userId", userId),
                status == null ? notDeleted() : equal("status", status));
    }

    /** One place's published reviews: what everybody sees. */
    public static Specification<Review> publishedOf(ReviewTargetType targetType, UUID targetId) {
        return Specification.allOf(
                equal("targetType", targetType),
                equal("targetId", targetId),
                equal("status", ReviewStatus.PUBLISHED));
    }

    public static Specification<Review> notDeleted() {
        return (root, query, builder) -> builder.notEqual(root.get("status"), ReviewStatus.DELETED);
    }

    private static Specification<Review> equal(String field, Object value) {
        return (root, query, builder) -> value == null ? null : builder.equal(root.get(field), value);
    }
}
