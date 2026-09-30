package ams.asmtravel.api.services.activity.recommendation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** @param limit how many places; null for the configured size */
public record RecommendationRequest(@Min(1) @Max(50) Integer limit) {
}
