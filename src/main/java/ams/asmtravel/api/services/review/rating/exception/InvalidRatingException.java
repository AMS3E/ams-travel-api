package ams.asmtravel.api.services.review.rating.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A rating the API will not accept (400). */
public class InvalidRatingException extends ResponseStatusException {

    public InvalidRatingException(String reason) {
        super(HttpStatus.BAD_REQUEST, reason);
    }
}
