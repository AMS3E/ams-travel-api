package ams.asmtravel.api.services.media.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** MinIO failed or refused (503). */
public class MediaStorageException extends ResponseStatusException {

    public MediaStorageException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
