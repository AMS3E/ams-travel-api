package ams.asmtravel.api.services.review.review.domain;

/** Only PUBLISHED reviews show on the site; the rest stay on record for the author and moderators. */
public enum ReviewStatus {
    /** Written and waiting for a moderator; not used when reviews publish at once. */
    PENDING,
    PUBLISHED,
    /** Taken off the site by a moderator; can be published again. */
    HIDDEN,
    /** Refused by a moderator. */
    REJECTED,
    /** Deleted by its author or a moderator; writing a review of the place again brings it back. */
    DELETED
}
