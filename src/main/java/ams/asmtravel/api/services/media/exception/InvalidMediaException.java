package ams.asmtravel.api.services.media.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A file, key or gallery change the API will not accept (400). */
public class InvalidMediaException extends ResponseStatusException {

    public InvalidMediaException(String reason) {
        super(HttpStatus.BAD_REQUEST, reason);
    }
}
