package ams.asmtravel.api.services.history.history.service;

import ams.asmtravel.api.services.history.history.dto.request.DestinationHistoryRequest;
import ams.asmtravel.api.services.history.history.dto.response.DestinationHistoryResponse;

import java.util.List;
import java.util.UUID;

/** Destination timelines. Published places are public; drafts only for their desks. */
public interface DestinationHistoryService {

    /** In date order; 404 for a place the caller may not see. */
    List<DestinationHistoryResponse> getTimeline(UUID destinationId);

    /** Every entry the caller may see: published places' for anyone, all of them for the back office. */
    List<DestinationHistoryResponse> getAll();

    DestinationHistoryResponse findById(UUID id);

    DestinationHistoryResponse create(DestinationHistoryRequest request);

    DestinationHistoryResponse update(UUID id, DestinationHistoryRequest request);

    void delete(UUID id);
}
