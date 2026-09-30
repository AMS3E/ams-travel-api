package ams.asmtravel.api.services.publishing.domain;

/** DRAFT → PENDING_REVIEW → APPROVED → PUBLISHED ⇄ UNPUBLISHED, or REJECTED. */
public enum PublishingStatus {
    DRAFT,
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    PUBLISHED,
    UNPUBLISHED;

    /** Waiting on its author or a reviewer: a listing has at most one. */
    public boolean isOpen() {
        return this == DRAFT || this == PENDING_REVIEW || this == APPROVED;
    }
}
