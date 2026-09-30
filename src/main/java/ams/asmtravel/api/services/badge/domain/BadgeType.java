package ams.asmtravel.api.services.badge.domain;

/** What a badge counts in one season. */
public enum BadgeType {
    /** Destinations opened, each place once a day. */
    BROWSES,
    /** Reviews written and still published. */
    REVIEWS,
    /** Digital stamps collected on site, anywhere. */
    STAMPS,
    /** Season points from ratings and reviews. */
    POINTS,
    /** Different places stamped among the destinations tied to the badge: a collection, on no ladder. */
    DESTINATION_STAMPS;

    /** Whether its badges form a SILVER → GOLDEN → PLATINUM ladder. */
    public boolean hasLevels() {
        return this != DESTINATION_STAMPS;
    }
}
