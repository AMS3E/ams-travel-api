package ams.asmtravel.api.services.badge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A badge definition or change the API will not accept (400). */
public class InvalidBadgeException extends ResponseStatusException {

    public InvalidBadgeException(String reason) {
        super(HttpStatus.BAD_REQUEST, reason);
    }
}
