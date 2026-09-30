package ams.asmtravel.api.services.history.period.service;

import ams.asmtravel.api.services.history.period.domain.HistoricalPeriod;
import ams.asmtravel.api.services.history.period.dto.request.HistoricalPeriodRequest;
import ams.asmtravel.api.services.history.period.dto.response.HistoricalPeriodResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The ages of Khmer history. */
public interface HistoricalPeriodService {

    /** Oldest first. */
    List<HistoricalPeriodResponse> getAll();

    HistoricalPeriodResponse findById(UUID id);

    HistoricalPeriodResponse create(HistoricalPeriodRequest request);

    HistoricalPeriodResponse update(UUID id, HistoricalPeriodRequest request);

    /** Styles and timeline rows in it stay, in no period. */
    void delete(UUID id);

    // For other services.

    boolean exists(UUID id);

    /** Those of these periods that exist, by id. */
    Map<UUID, HistoricalPeriodResponse> findAllById(Collection<UUID> ids);

    /** These periods, for a link to them; 400 names the first that does not exist. */
    Set<HistoricalPeriod> getEntities(Collection<UUID> ids);
}
