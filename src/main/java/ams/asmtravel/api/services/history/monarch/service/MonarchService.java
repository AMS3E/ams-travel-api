package ams.asmtravel.api.services.history.monarch.service;

import ams.asmtravel.api.services.history.monarch.dto.request.MonarchRequest;
import ams.asmtravel.api.services.history.monarch.dto.response.MonarchResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The Khmer kings behind the places in the catalogue. */
public interface MonarchService {

    /** In order of reign. */
    List<MonarchResponse> getAll();

    MonarchResponse findById(UUID id);

    MonarchResponse create(MonarchRequest request);

    MonarchResponse update(UUID id, MonarchRequest request);

    /** The places tied to them lose the tie. */
    void delete(UUID id);

    // For other services.

    /** Those of these monarchs that exist, by id. */
    Map<UUID, MonarchResponse> findAllById(Collection<UUID> ids);
}
