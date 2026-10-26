package ams.asmtravel.api.services.taxonomy.interestgroup.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such interest group (404). */
public class InterestGroupNotFoundException extends ResponseStatusException {

    public InterestGroupNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Interest group not found");
    }
}
