package ams.asmtravel.api.services.destination.destination.specification;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSearch;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.review.rating.domain.Rating;
import ams.asmtravel.api.services.review.rating.domain.RatingStatus;
import ams.asmtravel.api.services.review.rating.domain.RatingTargetType;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

/** The published catalogue narrowed by the app's search box and chips. */
public final class DestinationSpecification {

    private DestinationSpecification() {
    }

    public static Specification<Destination> published(DestinationSearch search) {
        return Specification.allOf(
                (root, query, builder) -> builder.equal(root.get("status"), ListingStatus.PUBLISHED),
                words(search.q()),
                inProvinces(search.provinceIds()),
                inRegions(search.regionIds()),
                anyOf("categories", search.categoryIds()),
                anyOf("tags", search.tagIds()),
                anyOf("interests", search.interestIds()),
                travelerSeal(search.seal()));
    }

    private static Specification<Destination> words(String q) {
        return (root, query, builder) -> {
            if (q == null || q.isBlank()) {
                return null;
            }
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("name")), like),
                    builder.like(builder.lower(root.get("shortDescription")), like),
                    builder.like(builder.lower(root.get("address")), like));
        };
    }

    private static Specification<Destination> inProvinces(Collection<UUID> provinceIds) {
        return (root, query, builder) -> provinceIds == null || provinceIds.isEmpty() ? null
                : root.get("province").get("id").in(provinceIds);
    }

    /** In a zone through its province, or shelved in a tourism region: either way the region matches. */
    private static Specification<Destination> inRegions(Collection<UUID> regionIds) {
        return (root, query, builder) -> {
            if (regionIds == null || regionIds.isEmpty()) {
                return null;
            }
            query.distinct(true);
            return builder.or(
                    root.get("province").get("region").get("id").in(regionIds),
                    root.join("tourismRegions", JoinType.LEFT).get("id").in(regionIds));
        };
    }

    private static Specification<Destination> anyOf(String relation, Collection<UUID> ids) {
        return (root, query, builder) -> {
            if (ids == null || ids.isEmpty()) {
                return null;
            }
            query.distinct(true);
            return root.join(relation).get("id").in(ids);
        };
    }

    /** Enough active star ratings at a high enough average: the same bar the Verified standing reads. */
    private static Specification<Destination> travelerSeal(DestinationSearch.Seal seal) {
        return (root, query, builder) -> {
            if (seal == null) {
                return null;
            }
            Subquery<Long> count = query.subquery(Long.class);
            Root<Rating> counted = count.from(Rating.class);
            count.select(builder.count(counted)).where(
                    builder.equal(counted.get("targetType"), RatingTargetType.DESTINATION),
                    builder.equal(counted.get("targetId"), root.get("id")),
                    builder.equal(counted.get("status"), RatingStatus.ACTIVE));
            Subquery<Double> average = query.subquery(Double.class);
            Root<Rating> averaged = average.from(Rating.class);
            average.select(builder.avg(averaged.<Short>get("stars"))).where(
                    builder.equal(averaged.get("targetType"), RatingTargetType.DESTINATION),
                    builder.equal(averaged.get("targetId"), root.get("id")),
                    builder.equal(averaged.get("status"), RatingStatus.ACTIVE));
            return builder.and(
                    builder.greaterThanOrEqualTo(count, seal.minRatings()),
                    builder.greaterThanOrEqualTo(average, seal.minAverage().doubleValue()));
        };
    }
}
