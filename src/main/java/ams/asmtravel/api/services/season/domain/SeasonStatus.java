package ams.asmtravel.api.services.season.domain;

/** Where a season is in its life; only one is ACTIVE at a time. */
public enum SeasonStatus {
    UPCOMING,
    ACTIVE,
    COMPLETED
}
