package ams.asmtravel.api.services.history.architecture.service;

import ams.asmtravel.api.services.history.architecture.domain.ArchitectureStyle;
import ams.asmtravel.api.services.history.architecture.dto.request.ArchitectureStyleRequest;
import ams.asmtravel.api.services.history.architecture.dto.response.ArchitectureStyleResponse;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The styles of Khmer building. */
public interface ArchitectureStyleService {

    /** By name; with a period, only that period's. */
    List<ArchitectureStyleResponse> getAll(UUID periodId);

    ArchitectureStyleResponse findById(UUID id);

    ArchitectureStyleResponse create(ArchitectureStyleRequest request);

    ArchitectureStyleResponse update(UUID id, ArchitectureStyleRequest request);

    /** The places built in it lose the tie. */
    void delete(UUID id);

    // For other services.

    /** Those of these styles that exist, by id. */
    Map<UUID, ArchitectureStyleResponse> findAllById(Collection<UUID> ids);

    /** The rows for these ids, for a place tied to them. @throws org.springframework.web.server.ResponseStatusException 400 for an id with none */
    Set<ArchitectureStyle> getEntities(Collection<UUID> ids);
}
