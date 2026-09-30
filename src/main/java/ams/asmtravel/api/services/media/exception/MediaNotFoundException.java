package ams.asmtravel.api.services.media.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such media item or file (404). */
public class MediaNotFoundException extends ResponseStatusException {

    public MediaNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Media not found");
    }
}
