package ams.asmtravel.api.services.notification.dto.response;

import ams.asmtravel.api.services.notification.domain.NotificationType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** @param data the ids the notification's link needs: a destination, a request, a badge */
public record NotificationResponse(
        UUID id,
        String title,
        String body,
        NotificationType type,
        Map<String, Object> data,
        Boolean isRead,
        Instant createdAt
) {
}
