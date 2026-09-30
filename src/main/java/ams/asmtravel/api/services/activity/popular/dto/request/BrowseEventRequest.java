package ams.asmtravel.api.services.activity.popular.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/**
 * Something the website saw a visitor do. The visitor comes from the token, never the body.
 *
 * @param eventType what happened, such as {@code PAGE_VIEW}
 * @param metadata  extra details: the path, the referrer
 */
public record BrowseEventRequest(
        UUID destinationId,
        @NotBlank @Size(max = 50) String eventType,
        @Size(max = 100) String sessionId,
        Map<String, Object> metadata
) {
}
