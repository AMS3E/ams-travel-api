package ams.asmtravel.api.services.location.region.service;

import ams.asmtravel.api.services.location.region.domain.Region;
import ams.asmtravel.api.services.location.region.domain.RegionKind;
import ams.asmtravel.api.services.location.region.dto.request.RegionRequest;
import ams.asmtravel.api.services.location.region.dto.response.RegionResponse;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Discover regions: tourism zones and themed tourism regions. */
public interface RegionService {

    /** Every region, or only the zones or only the themes when a kind is given. */
    List<RegionResponse> getAll(RegionKind kind);

    RegionResponse findById(UUID id);

    RegionResponse create(RegionRequest request);

    RegionResponse update(UUID id, RegionRequest request);

    /** @throws org.springframework.web.server.ResponseStatusException 409 while it holds provinces */
    void delete(UUID id);

    boolean exists(UUID id);

    /** Whether a tourism region has this code: what an attraction's theme names. */
    boolean isTourismCode(String code);

    /**
     * The row, for a province to belong to.
     *
     * @throws org.springframework.web.server.ResponseStatusException 400 when there is none
     */
    Region getEntity(UUID id);

    /**
     * The tourism regions a destination's form named.
     *
     * @throws org.springframework.web.server.ResponseStatusException 400 if one is missing or is a zone (zones come from the province)
     */
    Set<Region> getTourismEntities(Collection<UUID> ids);

    /** How many there are: a tile on the admin overview. */
    long count();
}
