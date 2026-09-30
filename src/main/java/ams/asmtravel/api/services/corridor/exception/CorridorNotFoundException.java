package ams.asmtravel.api.services.corridor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such corridor ({@code 404}). */
public class CorridorNotFoundException extends ResponseStatusException {

    public CorridorNotFoundException() {
        this("Corridor not found");
    }

    public CorridorNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
