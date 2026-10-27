package ams.asmtravel.api.services.verification.domain;

/**
 * What a destination's Verified badge rests on: there are two ways to earn it.
 * The first is a desk's decision, filed with the paperwork behind the payment;
 * the second is the travellers', worked out from their reviews and never
 * filed. Whatever the basis, the app shows the one Verified badge.
 */
public enum VerificationBasis {
    /** Paid to AMS Travel: the invoice or receipt is the evidence, renewed when a new one comes in. */
    PAID,
    /** Earned: enough star ratings at a high enough average. */
    TRAVELER_SCORE;

    /** Whether a desk files this basis, rather than the travellers earning it. */
    public boolean filed() {
        return this != TRAVELER_SCORE;
    }

    /** Whether the paperwork, the file or at least its number, has to come with it. */
    public boolean needsPaperwork() {
        return this == PAID;
    }
}
