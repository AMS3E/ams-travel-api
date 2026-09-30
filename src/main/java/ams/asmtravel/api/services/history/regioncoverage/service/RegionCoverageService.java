package ams.asmtravel.api.services.history.regioncoverage.service;

import ams.asmtravel.api.services.history.regioncoverage.dto.request.RegionCoverageRequest;
import ams.asmtravel.api.services.history.regioncoverage.dto.response.RegionCoverageResponse;

import java.util.List;
import java.util.UUID;

/** The succession a region is read as. */
public interface RegionCoverageService {

    /** One region's steps in order, or every region's when no region is named. */
    List<RegionCoverageResponse> getAll(UUID regionId);

    RegionCoverageResponse findById(UUID id);

    RegionCoverageResponse create(RegionCoverageRequest request);

    RegionCoverageResponse update(UUID id, RegionCoverageRequest request);

    void delete(UUID id);

    long count();
}
