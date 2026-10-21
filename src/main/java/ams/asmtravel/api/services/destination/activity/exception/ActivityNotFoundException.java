package ams.asmtravel.api.services.destination.activity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such activity: a plain {@code 404} {@link ResponseStatusException}. */
public class ActivityNotFoundException extends ResponseStatusException {

    public ActivityNotFoundException() {
        this("Activity not found");
    }

    public ActivityNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
