package ams.asmtravel.api.services.activity.recommendation.service;

import ams.asmtravel.api.services.activity.recommendation.dto.request.RecommendationRequest;
import ams.asmtravel.api.services.activity.recommendation.dto.response.RecommendationResponse;

import java.util.UUID;

/** Recommended for you: places like what this visitor browses, or what everyone browses. */
public interface RecommendationService {

    /** @param userId the signed-in visitor, or null for an anonymous one (who gets everyone's) */
    RecommendationResponse forVisitor(UUID userId, RecommendationRequest request);
}
