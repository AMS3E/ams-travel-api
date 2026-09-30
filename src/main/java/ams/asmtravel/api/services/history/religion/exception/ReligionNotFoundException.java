package ams.asmtravel.api.services.history.religion.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such religion: a plain 404 {@link ResponseStatusException}. */
public class ReligionNotFoundException extends ResponseStatusException {

    public ReligionNotFoundException() {
        this("Religion not found");
    }

    public ReligionNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
