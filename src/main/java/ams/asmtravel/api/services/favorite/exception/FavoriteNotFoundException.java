package ams.asmtravel.api.services.favorite.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such favorite (404). */
public class FavoriteNotFoundException extends ResponseStatusException {

    public FavoriteNotFoundException() {
        this("Not saved");
    }

    public FavoriteNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
