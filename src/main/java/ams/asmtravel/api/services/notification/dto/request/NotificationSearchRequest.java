package ams.asmtravel.api.services.notification.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * @param unreadOnly just the unread ones
 * @param limit      how many, newest first; null for 50
 */
public record NotificationSearchRequest(Boolean unreadOnly, @Min(1) @Max(200) Integer limit) {

    public int size() {
        return limit == null ? 50 : limit;
    }
}
