package ams.asmtravel.api.services.notification.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.notification.dto.request.NotificationSearchRequest;
import ams.asmtravel.api.services.notification.dto.response.NotificationResponse;
import ams.asmtravel.api.services.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** The caller's own notifications. */
@Tag(name = "Engagement · Notifications", description = "What the system told an account.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping
    public List<NotificationResponse> getMine(Authentication authentication,
                                              @Valid @ModelAttribute NotificationSearchRequest request) {
        return notificationService.findMine(userService.requireUserId(authentication), request);
    }

    @PutMapping("/{id}/read")
    public NotificationResponse markRead(Authentication authentication, @PathVariable UUID id) {
        return notificationService.markRead(userService.requireUserId(authentication), id);
    }

    @PutMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(Authentication authentication) {
        notificationService.markAllRead(userService.requireUserId(authentication));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable UUID id) {
        notificationService.delete(userService.requireUserId(authentication), id);
    }
}
