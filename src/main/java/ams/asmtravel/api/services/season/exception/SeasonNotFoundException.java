package ams.asmtravel.api.services.season.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such season (404). */
public class SeasonNotFoundException extends ResponseStatusException {

    public SeasonNotFoundException() {
        this("Season not found");
    }

    public SeasonNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
