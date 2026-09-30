package ams.asmtravel.api.services.history.monarch.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such monarch: a plain 404 {@link ResponseStatusException}. */
public class MonarchNotFoundException extends ResponseStatusException {

    public MonarchNotFoundException() {
        this("Monarch not found");
    }

    public MonarchNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
