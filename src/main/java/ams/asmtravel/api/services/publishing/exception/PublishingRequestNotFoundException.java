package ams.asmtravel.api.services.publishing.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such publishing request (404). */
public class PublishingRequestNotFoundException extends ResponseStatusException {

    public PublishingRequestNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Publishing request not found");
    }
}
