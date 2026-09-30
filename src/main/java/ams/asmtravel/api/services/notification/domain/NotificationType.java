package ams.asmtravel.api.services.notification.domain;

/** What a notification is about; the website picks its icon and link by it. */
public enum NotificationType {
    /** A publish request was approved: the place is live. */
    PUBLISH_APPROVED,
    /** A publish request was turned down. */
    PUBLISH_REJECTED,
    /** A publish request came back for changes. */
    PUBLISH_CHANGES_REQUESTED,
    /** A badge was earned. */
    BADGE_EARNED
}
