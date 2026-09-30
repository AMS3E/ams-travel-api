package ams.asmtravel.api.services.review.rating.domain;

/** A deleted rating stays on record but no longer counts; rating the place again brings it back. */
public enum RatingStatus {
    ACTIVE,
    DELETED
}
