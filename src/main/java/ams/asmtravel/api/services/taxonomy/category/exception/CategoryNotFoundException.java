package ams.asmtravel.api.services.taxonomy.category.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such category (404). */
public class CategoryNotFoundException extends ResponseStatusException {

    public CategoryNotFoundException() {
        this("Category not found");
    }

    public CategoryNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
