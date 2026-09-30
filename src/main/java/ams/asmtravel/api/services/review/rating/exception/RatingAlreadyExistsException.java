package ams.asmtravel.api.services.review.rating.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** The caller already rates this place; they change that rating instead (409). */
public class RatingAlreadyExistsException extends ResponseStatusException {

    public RatingAlreadyExistsException() {
        super(HttpStatus.CONFLICT, "You have already rated this place; change that rating instead");
    }
}
