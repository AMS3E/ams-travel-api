package ams.asmtravel.api.services.activity.popular.dto.response;

import ams.asmtravel.api.services.activity.popular.domain.PopularReason;

import java.util.List;
import java.util.UUID;

/**
 * @param days     how many days back the browses were counted, today included
 * @param regionId the zone asked about, or null
 */
public record PopularResponse(
        int days,
        UUID regionId,
        List<Place> destinations
) {

    /** @param browses different signed-in visitors in the window; 0 for a featured filler */
    public record Place(UUID destinationId, long browses, PopularReason reason) {
    }
}
