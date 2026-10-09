package ams.asmtravel.api.services.activity.recommendation.domain;

/** Why a place is recommended to this visitor. */
public enum RecommendationReason {
    /** Somewhere new to them, like what they have browsed. */
    LIKE_YOUR_BROWSING,
    /** What everyone browses, for a visitor with no taste to go on yet. */
    MOST_BROWSED
}
