package ams.asmtravel.api.services.history.period.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such historical period: a plain 404 {@link ResponseStatusException}. */
public class HistoricalPeriodNotFoundException extends ResponseStatusException {

    public HistoricalPeriodNotFoundException() {
        this("Historical period not found");
    }

    public HistoricalPeriodNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
