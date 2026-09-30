package ams.asmtravel.api.services.history.history.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such timeline entry: a plain 404 {@link ResponseStatusException}. */
public class DestinationHistoryNotFoundException extends ResponseStatusException {

    public DestinationHistoryNotFoundException() {
        this("Timeline entry not found");
    }

    public DestinationHistoryNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
