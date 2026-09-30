package ams.asmtravel.api.services.publishing.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A move the request's current status does not allow (409). */
public class InvalidPublishingStateException extends ResponseStatusException {

    public InvalidPublishingStateException(String reason) {
        super(HttpStatus.CONFLICT, reason);
    }
}
