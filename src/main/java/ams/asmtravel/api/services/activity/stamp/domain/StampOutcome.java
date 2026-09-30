package ams.asmtravel.api.services.activity.stamp.domain;

/** Result of a check-in. None is an error: too far is a normal answer. */
public enum StampOutcome {
    /** A new stamp. */
    COLLECTED,
    /** This place was already stamped today; one stamp per place per day. */
    ALREADY_COLLECTED,
    /** Outside the check-in radius. */
    TOO_FAR
}
