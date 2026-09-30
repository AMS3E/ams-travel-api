package ams.asmtravel.api.services.review.review.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** The caller already reviewed this place; they edit that review instead (409). */
public class ReviewAlreadyExistsException extends ResponseStatusException {

    public ReviewAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "You have already reviewed this place; edit that review instead");
    }
}
