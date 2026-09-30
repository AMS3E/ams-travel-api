package ams.asmtravel.api.services.review.rating.specification;

import ams.asmtravel.api.services.review.rating.domain.Rating;
import ams.asmtravel.api.services.review.rating.domain.RatingStatus;
import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/** Filters for listing ratings; a null value leaves that filter out. */
public final class RatingSpecification {

    private RatingSpecification() {
    }

    public static Specification<Rating> of(RatingTargetType targetType, UUID targetId, UUID userId, Integer stars) {
        return Specification.allOf(
                active(),
                equal("targetType", targetType),
                equal("targetId", targetId),
                equal("userId", userId),
                equal("stars", stars == null ? null : stars.shortValue()));
    }

    public static Specification<Rating> active() {
        return (root, query, builder) -> builder.equal(root.get("status"), RatingStatus.ACTIVE);
    }

    private static Specification<Rating> equal(String field, Object value) {
        return (root, query, builder) -> value == null ? null : builder.equal(root.get(field), value);
    }
}
