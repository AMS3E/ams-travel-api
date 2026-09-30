package ams.asmtravel.api.services.notification.service.impl;

import ams.asmtravel.api.services.badge.service.BadgeService.BadgeEarnedEvent;
import ams.asmtravel.api.services.notification.domain.Notification;
import ams.asmtravel.api.services.notification.domain.NotificationType;
import ams.asmtravel.api.services.notification.dto.request.NotificationSearchRequest;
import ams.asmtravel.api.services.notification.dto.response.NotificationResponse;
import ams.asmtravel.api.services.notification.exception.NotificationNotFoundException;
import ams.asmtravel.api.services.notification.mapper.NotificationMapper;
import ams.asmtravel.api.services.notification.repository.NotificationRepository;
import ams.asmtravel.api.services.notification.service.NotificationService;
import ams.asmtravel.api.services.publishing.domain.PublishingTargetType;
import ams.asmtravel.api.services.publishing.service.PublishingService.PublishingDecidedEvent;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> findMine(UUID userId, NotificationSearchRequest request) {
        PageRequest page = PageRequest.of(0, request.size());
        List<Notification> notifications = Boolean.TRUE.equals(request.unreadOnly())
                ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, page)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, page);
        return notifications.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional
    public NotificationResponse markRead(UUID userId, UUID id) {
        Notification notification = find(userId, id);
        notification.setIsRead(true);
        return mapper.toResponse(notification);
    }

    @Override
    @Transactional
    public int markAllRead(UUID userId) {
        return notificationRepository.markAllRead(userId);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID id) {
        notificationRepository.delete(find(userId, id));
    }

    @Override
    @Transactional
    public NotificationResponse notify(UUID userId, NotificationType type, String title, String body, Map<String, Object> data) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title.length() > 255 ? title.substring(0, 255) : title);
        notification.setBody(body);
        notification.setData(data);
        return mapper.toResponse(notificationRepository.save(notification));
    }

    @EventListener
    public void onPublishDecided(PublishingDecidedEvent event) {
        String place = event.targetName();
        NotificationType type;
        String title;
        switch (event.status()) {
            case APPROVED -> {
                type = NotificationType.PUBLISH_APPROVED;
                title = place + " was approved";
            }
            case PUBLISHED -> {
                type = NotificationType.PUBLISH_APPROVED;
                title = place + " is live";
            }
            case REJECTED -> {
                type = NotificationType.PUBLISH_REJECTED;
                title = place + " was not approved";
            }
            case DRAFT -> {
                type = NotificationType.PUBLISH_CHANGES_REQUESTED;
                title = place + " needs changes";
            }
            default -> {
                return;
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requestId", event.requestId().toString());
        data.put("listingType", event.targetType().name());
        data.put("listingId", event.targetId().toString());
        if (event.targetType() == PublishingTargetType.DESTINATION) {
            data.put("destinationId", event.targetId().toString());
        }
        this.notify(event.requesterId(), type, title, event.note(), data);
    }

    @EventListener
    public void onBadgeEarned(BadgeEarnedEvent event) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userBadgeId", event.userBadgeId().toString());
        this.notify(event.userId(), NotificationType.BADGE_EARNED,
                "You earned " + event.badgeName(), "In " + event.seasonName() + ".", data);
    }

    @EventListener
    public void onUserDeleting(UserDeletingEvent event) {
        notificationRepository.deleteByUserId(event.userId());
    }

    private Notification find(UUID userId, UUID id) {
        return notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotificationNotFoundException());
    }
}
