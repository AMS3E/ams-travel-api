package ams.asmtravel.api.services.taxonomy.interest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such interest (404). */
public class InterestNotFoundException extends ResponseStatusException {

    public InterestNotFoundException() {
        this("Interest not found");
    }

    public InterestNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
