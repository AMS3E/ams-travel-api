package ams.asmtravel.api.services.activity.recommendation.domain;

import java.util.Set;
import java.util.UUID;

/** What a visitor likes, from the categories, interests and zones of the places they opened. */
public record Taste(Set<UUID> categoryIds, Set<UUID> interestIds, Set<UUID> regionIds) {

    public Taste {
        categoryIds = Set.copyOf(categoryIds);
        interestIds = Set.copyOf(interestIds);
        regionIds = Set.copyOf(regionIds);
    }

    /** A visitor who has browsed nothing yet, or nothing still published. */
    public boolean isEmpty() {
        return categoryIds.isEmpty() && interestIds.isEmpty() && regionIds.isEmpty();
    }

    /** One for each shared category and interest, one more for the same zone; zero is nothing alike. */
    public int match(Set<UUID> placeCategoryIds, Set<UUID> placeInterestIds, UUID placeRegionId) {
        int shared = (int) placeCategoryIds.stream().filter(categoryIds::contains).count()
                + (int) placeInterestIds.stream().filter(interestIds::contains).count();
        return shared + (placeRegionId != null && regionIds.contains(placeRegionId) ? 1 : 0);
    }
}
