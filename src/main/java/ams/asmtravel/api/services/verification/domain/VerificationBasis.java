package ams.asmtravel.api.services.verification.domain;

/**
 * What a destination's Verified badge rests on. The first three are a desk's
 * decision, filed with what each needs; the fourth is the travellers', worked
 * out from their reviews and never filed. A verified place shows as
 * Recommended by AMS Travel in the app; the fourth adds Recommended by Traveler.
 */
public enum VerificationBasis {
    /** A national or official site: verified on its standing, no paperwork. */
    OFFICIAL_SITE,
    /** Partnered with AMS Travel: the agreement is the evidence, and the place has to be a partner's own. */
    PARTNER,
    /** Paid for: the invoice or receipt is the evidence, renewed when a new one comes in. */
    PAID,
    /** Earned: enough star ratings at a high enough average. */
    TRAVELER_SCORE;

    /** Whether a desk files this basis, rather than the travellers earning it. */
    public boolean filed() {
        return this != TRAVELER_SCORE;
    }

    /** Whether the paperwork, the file or at least its number, has to come with it. */
    public boolean needsPaperwork() {
        return this == PARTNER || this == PAID;
    }
}
