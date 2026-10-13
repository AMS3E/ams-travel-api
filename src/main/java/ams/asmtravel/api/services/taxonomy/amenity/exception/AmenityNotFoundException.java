package ams.asmtravel.api.services.taxonomy.amenity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such amenity (404). */
public class AmenityNotFoundException extends ResponseStatusException {

    public AmenityNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Amenity not found");
    }
}
