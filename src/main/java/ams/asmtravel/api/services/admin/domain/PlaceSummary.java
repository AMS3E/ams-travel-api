package ams.asmtravel.api.services.admin.domain;

import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.domain.ListingStatus;
import ams.asmtravel.api.services.destination.food.dto.response.FoodResponse;
import ams.asmtravel.api.services.destination.stay.dto.response.StayResponse;

import java.util.UUID;

/** What the stats need to know of a place of any kind, as of today; a stay has no slug. */
public record PlaceSummary(PlaceType type, UUID id, String name, String slug, UUID provinceId, ListingStatus status, long viewCount) {

    public static PlaceSummary of(DestinationSummary place) {
        return new PlaceSummary(PlaceType.DESTINATION, place.id(), place.name(), place.slug(), place.provinceId(), place.status(),
                place.viewCount());
    }

    public static PlaceSummary of(StayResponse stay) {
        return new PlaceSummary(PlaceType.STAY, stay.id(), stay.name(), null, stay.provinceId(), stay.status(),
                stay.viewCount() == null ? 0 : stay.viewCount());
    }

    public static PlaceSummary of(FoodResponse food) {
        return new PlaceSummary(PlaceType.FOOD, food.id(), food.name(), food.slug(), food.provinceId(), food.status(),
                food.viewCount() == null ? 0 : food.viewCount());
    }
}
