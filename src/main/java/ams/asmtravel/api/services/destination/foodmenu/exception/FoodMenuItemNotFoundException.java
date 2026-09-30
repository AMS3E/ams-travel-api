package ams.asmtravel.api.services.destination.foodmenu.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such dish — or one on a listing the caller may not see yet. */
public class FoodMenuItemNotFoundException extends ResponseStatusException {

    public FoodMenuItemNotFoundException() {
        super(HttpStatus.NOT_FOUND, "Dish not found");
    }
}
