package ams.asmtravel.api.services.verification.domain;

import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.food.dto.response.FoodResponse;
import ams.asmtravel.api.services.destination.stay.dto.response.StayResponse;
import ams.asmtravel.api.services.publishing.domain.PublishingTargetType;

import java.util.UUID;

/**
 * A destination of any kind as verification sees it. The author decides the
 * desk: who wrote an attraction, or the partner a stay or a food place is
 * listed for; a stay has no slug.
 */
public record VerifiedPlace(PublishingTargetType type, UUID id, String name, String slug, String coverKey, ListingStatus status,
                            UUID authorId) {

    public static VerifiedPlace of(DestinationSummary place) {
        return new VerifiedPlace(PublishingTargetType.DESTINATION, place.id(), place.name(), place.slug(), place.coverKey(),
                place.status(), place.createdBy());
    }

    public static VerifiedPlace of(StayResponse stay) {
        return new VerifiedPlace(PublishingTargetType.STAY, stay.id(), stay.name(), null, stay.coverKey(), stay.status(),
                stay.ownerUserId());
    }

    public static VerifiedPlace of(FoodResponse food) {
        return new VerifiedPlace(PublishingTargetType.FOOD, food.id(), food.name(), food.slug(), food.coverKey(), food.status(),
                food.ownerUserId() != null ? food.ownerUserId() : food.createdBy());
    }

    public boolean published() {
        return status == ListingStatus.PUBLISHED;
    }
}
