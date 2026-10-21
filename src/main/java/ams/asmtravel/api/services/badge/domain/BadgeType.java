package ams.asmtravel.api.services.badge.domain;

/** What a badge counts in one season. Attractions, stays and food places are all destinations here. */
public enum BadgeType {
    /** Destinations opened, each place once a day. */
    BROWSES,
    /** Reviews written and still published; one per place, since a person reviews a place once. */
    REVIEWS,
    /** Digital stamps collected on site, one per place per season. */
    STAMPS,
    /** Different places stamped among the destinations tied to the badge: a collection, on no ladder. */
    DESTINATION_STAMPS;

    /** Whether its badges form a SILVER → GOLDEN → PLATINUM ladder. */
    public boolean hasLevels() {
        return this != DESTINATION_STAMPS;
    }
}
