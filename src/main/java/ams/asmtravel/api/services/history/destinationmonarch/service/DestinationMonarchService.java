package ams.asmtravel.api.services.history.destinationmonarch.service;

import ams.asmtravel.api.services.history.destinationmonarch.dto.request.DestinationMonarchRequest;
import ams.asmtravel.api.services.history.destinationmonarch.dto.response.DestinationMonarchResponse;

import java.util.List;
import java.util.UUID;

/** Monarchs tied to destinations. Published places are public; drafts only for their desks. */
public interface DestinationMonarchService {

    /** One place's monarchs, earliest reign first (404 if hidden), or every tie the caller may see. */
    List<DestinationMonarchResponse> getAll(UUID destinationId);

    /** Replaces a place's monarchs whole. */
    List<DestinationMonarchResponse> replace(UUID destinationId, DestinationMonarchRequest request);
}
