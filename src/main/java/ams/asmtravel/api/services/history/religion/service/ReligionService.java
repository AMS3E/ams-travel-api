package ams.asmtravel.api.services.history.religion.service;

import ams.asmtravel.api.services.history.religion.domain.Religion;
import ams.asmtravel.api.services.history.religion.dto.request.ReligionRequest;
import ams.asmtravel.api.services.history.religion.dto.response.ReligionResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The faiths the places in the catalogue were built for. */
public interface ReligionService {

    List<ReligionResponse> getAll();

    ReligionResponse findById(UUID id);

    ReligionResponse create(ReligionRequest request);

    ReligionResponse update(UUID id, ReligionRequest request);

    /** The places tied to it lose the tie. */
    void delete(UUID id);

    // For other services.

    /** Those of these religions that exist, by id. */
    Map<UUID, ReligionResponse> findAllById(Collection<UUID> ids);

    /** The rows for these ids, for a place tied to them. @throws org.springframework.web.server.ResponseStatusException 400 for an id with none */
    Set<Religion> getEntities(Collection<UUID> ids);
}
