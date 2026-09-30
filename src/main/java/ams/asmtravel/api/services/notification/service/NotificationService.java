package ams.asmtravel.api.services.notification.service;

import ams.asmtravel.api.services.notification.domain.NotificationType;
import ams.asmtravel.api.services.notification.dto.request.NotificationSearchRequest;
import ams.asmtravel.api.services.notification.dto.response.NotificationResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A person's notifications, made by listeners for events other services publish. */
public interface NotificationService {

    /** Newest first. */
    List<NotificationResponse> findMine(UUID userId, NotificationSearchRequest request);

    /** 404 for one that is not the caller's. */
    NotificationResponse markRead(UUID userId, UUID id);

    /** @return how many were unread */
    int markAllRead(UUID userId);

    void delete(UUID userId, UUID id);

    /** Writes one down. */
    NotificationResponse notify(UUID userId, NotificationType type, String title, String body, Map<String, Object> data);
}
