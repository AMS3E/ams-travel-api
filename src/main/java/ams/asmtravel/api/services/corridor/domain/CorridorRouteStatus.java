package ams.asmtravel.api.services.corridor.domain;

/** How the stored road route stands against the itinerary as it is now. */
public enum CorridorRouteStatus {
    /** Fewer than two stops. */
    NONE,
    /** Not calculated for these stops yet. */
    MISSING,
    /** Matches the current stops exactly. */
    CURRENT,
    /** A stop, its position or its transport changed since. */
    STALE
}
