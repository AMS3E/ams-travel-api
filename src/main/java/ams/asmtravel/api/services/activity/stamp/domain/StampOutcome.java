package ams.asmtravel.api.services.activity.stamp.domain;

/** What a check-in answered. */
public enum StampOutcome {
    /** A new stamp. */
    COLLECTED,
    /** This place was already stamped this season; one stamp per place per season. */
    ALREADY_COLLECTED,
    /** Outside the check-in radius. */
    TOO_FAR,
    /** No current code reads like this: mistyped, withdrawn, or replaced by a newer print. */
    INVALID_CODE
}
