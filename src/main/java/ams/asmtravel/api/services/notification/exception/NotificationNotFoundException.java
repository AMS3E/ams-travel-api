package ams.asmtravel.api.services.notification.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such notification (404). */
public class NotificationNotFoundException extends ResponseStatusException {

    public NotificationNotFoundException() {
        this("Notification not found");
    }

    public NotificationNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
