package ams.asmtravel.api.services.activity.recommendation.dto.response;

import ams.asmtravel.api.services.activity.recommendation.domain.RecommendationReason;

import java.util.List;
import java.util.UUID;

/** @param personal whether the list comes from this visitor's browsing, or from everyone's */
public record RecommendationResponse(
        boolean personal,
        List<Place> destinations
) {

    public record Place(UUID destinationId, long browses, RecommendationReason reason) {
    }
}
