package ams.asmtravel.api.services.destination.stay.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such stay: a plain {@code 404} {@link ResponseStatusException}. */
public class StayNotFoundException extends ResponseStatusException {

    public StayNotFoundException() {
        this("Stay not found");
    }

    public StayNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
