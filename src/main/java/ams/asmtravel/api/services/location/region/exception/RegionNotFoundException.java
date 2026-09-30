package ams.asmtravel.api.services.location.region.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such region: a plain 404 {@link ResponseStatusException}. */
public class RegionNotFoundException extends ResponseStatusException {

    public RegionNotFoundException() {
        this("Region not found");
    }

    public RegionNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
