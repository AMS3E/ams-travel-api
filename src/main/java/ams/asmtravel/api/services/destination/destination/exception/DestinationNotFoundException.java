package ams.asmtravel.api.services.destination.destination.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such destination: a plain {@code 404} {@link ResponseStatusException}. */
public class DestinationNotFoundException extends ResponseStatusException {

    public DestinationNotFoundException() {
        this("Destination not found");
    }

    public DestinationNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
