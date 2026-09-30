package ams.asmtravel.api.services.location.province.service;

import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.province.dto.request.ProvinceRequest;
import ams.asmtravel.api.services.location.province.dto.response.ProvinceResponse;

import java.util.List;
import java.util.UUID;

/** Cambodia's 25 provinces, each in one tourism zone. */
public interface ProvinceService {

    List<ProvinceResponse> getAll();

    ProvinceResponse findById(UUID id);

    ProvinceResponse create(ProvinceRequest request);

    ProvinceResponse update(UUID id, ProvinceRequest request);

    /** @throws org.springframework.web.server.ResponseStatusException 409 while destinations are in it */
    void delete(UUID id);

    /**
     * The row, for a destination to belong to.
     *
     * @throws org.springframework.web.server.ResponseStatusException 400 when there is none
     */
    Province getEntity(UUID id);

    /** How many there are: a tile on the admin overview. */
    long count();
}
