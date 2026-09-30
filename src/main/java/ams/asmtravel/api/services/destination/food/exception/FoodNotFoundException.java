package ams.asmtravel.api.services.destination.food.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such food listing: a plain {@code 404} {@link ResponseStatusException}. */
public class FoodNotFoundException extends ResponseStatusException {

    public FoodNotFoundException() {
        this("Food not found");
    }

    public FoodNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
