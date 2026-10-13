package ams.asmtravel.api.services.taxonomy.amenity.service;

import ams.asmtravel.api.services.taxonomy.amenity.domain.AmenityLevel;
import ams.asmtravel.api.services.taxonomy.amenity.dto.request.AmenityRequest;
import ams.asmtravel.api.services.taxonomy.amenity.dto.response.AmenityResponse;

import java.util.List;
import java.util.UUID;

public interface AmenityService {

    /** Every amenity, retired ones included, group by group; a level narrows to the ones offered there. */
    List<AmenityResponse> getAll(AmenityLevel level);

    AmenityResponse findById(UUID id);

    AmenityResponse create(AmenityRequest request);

    AmenityResponse update(UUID id, AmenityRequest request);

    /**
     * Deletes an amenity nothing lists any more.
     *
     * @throws org.springframework.web.server.ResponseStatusException 409 while a stay or a room still lists it: retire it instead
     */
    void delete(UUID id);

    /**
     * The given slugs no amenity offered at this level answers to, A to Z.
     * A retired amenity still answers, so a stay saved before it was retired keeps it.
     */
    List<String> unknown(List<String> slugs, AmenityLevel level);

    /** How many there are: a tile on the admin overview. */
    long count();
}
