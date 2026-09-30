package ams.asmtravel.api.services.activity.popular.domain;

import java.util.UUID;

/** How many different signed-in visitors opened one place in a window. */
public record BrowseTotal(UUID destinationId, Long browses) {
}
