package ams.asmtravel.api.services.activity.popular.domain;

/** The event types the API writes itself; the website may send others. */
public final class BrowseKinds {

    /** A signed-in visitor opened a destination's page, as the badge service records it. */
    public static final String EXPLORATION = "EXPLORATION";

    private BrowseKinds() {
    }
}
