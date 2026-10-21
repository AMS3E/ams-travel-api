package ams.asmtravel.api.services.activity.stamp.domain;

/** How a check-in was proven. */
public enum StampSource {
    /** Before 2026-10-20: the visitor's device reported standing within the radius. */
    GPS,
    /** The visitor scanned the code posted on site and stood within the radius. */
    QR
}
