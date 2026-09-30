package ams.asmtravel.api.services.history.architecture.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such architecture style: a plain 404 {@link ResponseStatusException}. */
public class ArchitectureStyleNotFoundException extends ResponseStatusException {

    public ArchitectureStyleNotFoundException() {
        this("Architecture style not found");
    }

    public ArchitectureStyleNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
