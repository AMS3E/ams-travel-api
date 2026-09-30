package ams.asmtravel.api.services.season.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A season change the API refuses: 409 for its state or a clash, 400 for bad input. */
public class InvalidSeasonException extends ResponseStatusException {

    public InvalidSeasonException(String reason) {
        super(HttpStatus.CONFLICT, reason);
    }

    public InvalidSeasonException(HttpStatus status, String reason) {
        super(status, reason);
    }
}
