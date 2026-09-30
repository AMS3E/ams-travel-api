package ams.asmtravel.api.services.user.user.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such staff account (404). */
public class StaffAccountNotFoundException extends ResponseStatusException {

    public StaffAccountNotFoundException() {
        this("Account not found");
    }

    public StaffAccountNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
