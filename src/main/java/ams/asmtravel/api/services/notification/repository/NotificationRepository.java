package ams.asmtravel.api.services.notification.repository;

import ams.asmtravel.api.services.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable page);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId, Pageable page);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("update Notification n set n.isRead = true where n.userId = :userId and n.isRead = false")
    int markAllRead(UUID userId);

    @Modifying
    @Query("delete from Notification n where n.userId = :userId")
    int deleteByUserId(UUID userId);
}
