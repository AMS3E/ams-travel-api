package ams.asmtravel.api.services.activity.popular.dto.response;

import ams.asmtravel.api.services.activity.popular.domain.BrowseTarget;
import ams.asmtravel.api.services.activity.popular.domain.PopularReason;

import java.util.List;
import java.util.UUID;

/**
 * The Popular list of one kind of place.
 *
 * @param days         how many days back browses are counted
 * @param regionId     the one region asked for, when one was
 * @param type         the kind every item is
 * @param items        most browsed first, then the filler ({@code reason} says which), as many as asked when the catalogue allows
 * @param destinations the older shape of the same rows, filled when the kind is DESTINATION
 */
public record PopularResponse(
        int days,
        UUID regionId,
        BrowseTarget type,
        List<Item> items,
        List<Place> destinations
) {
    public record Item(BrowseTarget type, UUID id, long browses, PopularReason reason) {
    }

    public record Place(UUID destinationId, long browses, PopularReason reason) {
    }
}
