package ams.asmtravel.api.services.corridor.exception;

import org.springframework.http.HttpStatusCode;

/**
 * Google Routes refused, failed or could not be reached.
 *
 * @see CorridorRouteException
 */
public class GoogleRoutesException extends RuntimeException {

    private final HttpStatusCode googleStatus;
    private final String googleReason;

    public GoogleRoutesException(HttpStatusCode googleStatus, String googleReason, String message, Throwable cause) {
        super(message, cause);
        this.googleStatus = googleStatus;
        this.googleReason = googleReason;
    }

    /** Google's HTTP status; null when no answer came. */
    public HttpStatusCode googleStatus() {
        return googleStatus;
    }

    /** Google's status word (e.g. PERMISSION_DENIED), if given. */
    public String googleReason() {
        return googleReason;
    }
}
