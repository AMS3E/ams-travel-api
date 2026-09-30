package ams.asmtravel.api.services.corridor.exception;

import org.springframework.http.HttpStatusCode;

/**
 * The OpenStreetMap routing server refused, failed or could not be reached.
 *
 * @see CorridorRouteException
 */
public class OsmRoutesException extends RuntimeException {

    private final HttpStatusCode serverStatus;

    public OsmRoutesException(HttpStatusCode serverStatus, String message, Throwable cause) {
        super(message, cause);
        this.serverStatus = serverStatus;
    }

    /** The server's HTTP status; null when no answer came. */
    public HttpStatusCode serverStatus() {
        return serverStatus;
    }
}
