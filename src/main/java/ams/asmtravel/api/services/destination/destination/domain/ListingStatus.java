package ams.asmtravel.api.services.destination.destination.domain;

/** Life stage of a destination, stay or food listing. Only PUBLISHED rows are public. */
public enum ListingStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
