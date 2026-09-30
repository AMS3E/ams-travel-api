package ams.asmtravel.api.services.review.rating.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such rating, or nothing published to rate (404). */
public class RatingNotFoundException extends ResponseStatusException {

    public RatingNotFoundException() {
        this("Rating not found");
    }

    public RatingNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
