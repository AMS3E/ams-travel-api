package ams.asmtravel.api.services.location.province.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such province: a plain 404 {@link ResponseStatusException}. */
public class ProvinceNotFoundException extends ResponseStatusException {

    public ProvinceNotFoundException() {
        this("Province not found");
    }

    public ProvinceNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
