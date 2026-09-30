package ams.asmtravel.api.services.review.review.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** A review or review change the API will not accept (400). */
public class InvalidReviewException extends ResponseStatusException {

    public InvalidReviewException(String reason) {
        super(HttpStatus.BAD_REQUEST, reason);
    }
}
