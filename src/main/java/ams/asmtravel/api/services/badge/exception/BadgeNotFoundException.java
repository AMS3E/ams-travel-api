package ams.asmtravel.api.services.badge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such badge (404). */
public class BadgeNotFoundException extends ResponseStatusException {

    public BadgeNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Badge not found");
    }
}
