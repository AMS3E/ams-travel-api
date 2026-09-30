package ams.asmtravel.api.services.activity.recommendation.domain;

import java.util.UUID;

/** One recommended place, in list order. */
public record RankedDestination(UUID destinationId, long browses, RecommendationReason reason) {
}
