package ams.asmtravel.api.services.taxonomy.tag.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such tag (404). */
public class TagNotFoundException extends ResponseStatusException {

    public TagNotFoundException() {
        this("Tag not found");
    }

    public TagNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
