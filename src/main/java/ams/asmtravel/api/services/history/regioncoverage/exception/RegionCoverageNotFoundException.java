package ams.asmtravel.api.services.history.regioncoverage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** No such coverage entry: a plain 404 {@link ResponseStatusException}. */
public class RegionCoverageNotFoundException extends ResponseStatusException {

    public RegionCoverageNotFoundException() {
        this("Coverage entry not found");
    }

    public RegionCoverageNotFoundException(String reason) {
        super(HttpStatus.NOT_FOUND, reason);
    }
}
