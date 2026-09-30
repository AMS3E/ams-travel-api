package ams.asmtravel.api.services.taxonomy.interest.service;

import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import ams.asmtravel.api.services.taxonomy.interest.dto.request.InterestRequest;
import ams.asmtravel.api.services.taxonomy.interest.dto.response.InterestResponse;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface InterestService {

    List<InterestResponse> getAll();

    InterestResponse findById(UUID id);

    InterestResponse create(InterestRequest request);

    InterestResponse update(UUID id, InterestRequest request);

    /** Destinations and food lose the interest. */
    void delete(UUID id);

    /** @throws org.springframework.web.server.ResponseStatusException 400 when any id is unknown */
    Set<Interest> getEntities(Collection<UUID> ids);

    /** How many there are: a tile on the admin overview. */
    long count();
}
