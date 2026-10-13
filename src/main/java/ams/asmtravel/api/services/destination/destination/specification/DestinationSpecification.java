package ams.asmtravel.api.services.destination.destination.specification;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSearch;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

/** The app's search over published places, as a database query; every filter is optional. */
public final class DestinationSpecification {

    private DestinationSpecification() {
    }

    public static Specification<Destination> published(DestinationSearch search) {
        return Specification.allOf(
                (root, query, builder) -> builder.equal(root.get("status"), ListingStatus.PUBLISHED),
                words(search.q()),
                inProvince(search.provinceId()),
                linkedTo("tourismRegions", search.regionId()),
                anyOf("categories", search.categoryIds()),
                anyOf("tags", search.tagIds()),
                anyOf("interests", search.interestIds()));
    }

    /** The words anywhere in the name, the short description or the address, whatever the case. */
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

    private static Specification<Destination> inProvince(UUID provinceId) {
        return (root, query, builder) -> provinceId == null ? null : builder.equal(root.get("province").get("id"), provinceId);
    }

    /** Shelved in the one region: a join, so the page is counted and cut on distinct places. */
    private static Specification<Destination> linkedTo(String relation, UUID id) {
        return (root, query, builder) -> {
            if (id == null) {
                return null;
            }
            query.distinct(true);
            return builder.equal(root.join(relation).get("id"), id);
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
}
