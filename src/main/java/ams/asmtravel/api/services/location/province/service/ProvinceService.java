package ams.asmtravel.api.services.location.province.service;

import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.province.domain.ProvinceSummary;
import ams.asmtravel.api.services.location.province.dto.request.ProvinceRequest;
import ams.asmtravel.api.services.location.province.dto.response.ProvinceResponse;

import java.util.List;
import java.util.UUID;

public interface ProvinceService {

    /** Every province with what its places add up to: the app's province cards. */
    List<ProvinceResponse> getAll();

    ProvinceResponse findById(UUID id);

    ProvinceResponse create(ProvinceRequest request);

    ProvinceResponse update(UUID id, ProvinceRequest request);

    void delete(UUID id);

    // For other services.

    /** Names, zones and centres only, A to Z: what a lookup or a filter needs, without the counting. */
    List<ProvinceSummary> findSummaries();

    boolean exists(UUID id);

    Province getEntity(UUID id);

    long count();
}
