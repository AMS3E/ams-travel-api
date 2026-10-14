package ams.asmtravel.api.services.activity.popular.dto.request;

import ams.asmtravel.api.services.activity.popular.domain.BrowseTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/**
 * A page opened in the app.
 *
 * @param targetType    DESTINATION (when left out), STAY, FOOD or CORRIDOR
 * @param targetId      the page's id
 * @param destinationId the older name for a destination's id; {@code targetId} says the same
 * @param eventType     what happened, e.g. {@code VIEW}
 * @param sessionId     the app's session, so an anonymous visitor's repeat opens count once a day
 */
public record BrowseEventRequest(
        BrowseTarget targetType,
        UUID targetId,
        UUID destinationId,
        @NotBlank @Size(max = 50) String eventType,
        @Size(max = 100) String sessionId,
        Map<String, Object> metadata
) {
    public BrowseTarget resolvedType() {
        return targetType == null ? BrowseTarget.DESTINATION : targetType;
    }

    public UUID resolvedTarget() {
        return targetId != null ? targetId : destinationId;
    }
}
