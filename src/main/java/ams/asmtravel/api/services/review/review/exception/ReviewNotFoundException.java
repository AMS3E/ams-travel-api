package ams.asmtravel.api.services.review.review.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such review (404). */
public class ReviewNotFoundException extends ResponseStatusException {

    public ReviewNotFoundException() {
        this("Review not found");
    }

    public ReviewNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
