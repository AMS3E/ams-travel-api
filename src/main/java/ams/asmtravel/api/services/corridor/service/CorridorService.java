package ams.asmtravel.api.services.corridor.service;

import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.corridor.dto.request.CorridorRequest;
import ams.asmtravel.api.services.corridor.dto.response.CorridorResponse;

import java.util.List;
import java.util.UUID;

/** Corridor overview and publishing. Anyone reads published ones; only curators see drafts and write. */
public interface CorridorService {

    /** Published corridors (all for a curator); {@code slug} narrows to one. */
    List<CorridorResponse> getAll(String slug);

    /** @throws org.springframework.web.server.ResponseStatusException 404 if missing or a hidden draft */
    CorridorResponse findById(UUID id);

    /** A new draft. */
    CorridorResponse create(CorridorRequest request, UUID authorId);

    CorridorResponse update(UUID id, CorridorRequest request);

    /**
     * Publishes the corridor; its route is calculated after commit.
     *
     * @throws org.springframework.web.server.ResponseStatusException 400 if a stop's destination is not published
     */
    void publish(UUID id);

    void unpublish(UUID id);

    /** Also removes its days, stops and favorites. */
    void delete(UUID id);

    /** @throws org.springframework.web.server.ResponseStatusException 404 when there is none */
    Corridor getEntity(UUID id);

    boolean exists(UUID id);

    /** Only published corridors can be saved as favorites. */
    boolean isPublished(UUID id);

    long count();
}
