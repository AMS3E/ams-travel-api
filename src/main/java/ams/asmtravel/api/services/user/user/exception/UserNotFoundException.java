package ams.asmtravel.api.services.user.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such user (404). */
public class UserNotFoundException extends ResponseStatusException {

    public UserNotFoundException() {
        this("User not found");
    }

    public UserNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
