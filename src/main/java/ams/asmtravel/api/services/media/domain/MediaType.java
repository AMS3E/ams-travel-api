package ams.asmtravel.api.services.media.domain;

import java.util.Locale;

/** What a file is for: it names its MinIO folder, and for a gallery photo what it belongs to. */
public enum MediaType {
    AVATAR,
    REVIEW,
    DESTINATION,
    STAY,
    FOOD,
    CORRIDOR,
    PARTNER,
    VERIFICATION,
    GENERAL;

    /** The folder in the bucket, e.g. {@code destination}. */
    public String folder() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Anyone signed in may upload these; the rest is catalogue media. */
    public boolean isPersonal() {
        return this == AVATAR || this == REVIEW;
    }

    /** Verification evidence: read only by the verifying desks. */
    public boolean isPrivate() {
        return this == VERIFICATION;
    }

    /** Has a gallery at /api/v1/media; review photos come with their review. */
    public boolean hasGallery() {
        return this == DESTINATION || this == STAY || this == FOOD;
    }
}
