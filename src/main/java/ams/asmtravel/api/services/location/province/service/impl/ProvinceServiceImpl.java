package ams.asmtravel.api.services.location.province.service.impl;

import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.province.domain.ProvinceDeletingEvent;
import ams.asmtravel.api.services.location.province.dto.request.ProvinceRequest;
import ams.asmtravel.api.services.location.province.dto.response.ProvinceResponse;
import ams.asmtravel.api.services.location.province.exception.ProvinceNotFoundException;
import ams.asmtravel.api.services.location.province.mapper.ProvinceMapper;
import ams.asmtravel.api.services.location.province.repository.ProvinceRepository;
import ams.asmtravel.api.services.location.province.service.ProvinceService;
import ams.asmtravel.api.services.location.region.domain.Region;
import ams.asmtravel.api.services.location.region.domain.RegionDeletingEvent;
import ams.asmtravel.api.services.location.region.domain.RegionKind;
import ams.asmtravel.api.services.location.region.service.RegionService;
import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.authorization.domain.Authorities;
import ams.asmtravel.api.services.user.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProvinceServiceImpl implements ProvinceService {

    private final ProvinceRepository provinceRepository;
    private final RegionService regionService;
    private final ProvinceMapper mapper;
    private final UserService userService;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<ProvinceResponse> getAll() {
        return provinceRepository.findAllByOrderByNameAsc().stream().map(mapper::toProvinceResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProvinceResponse findById(UUID id) {
        return mapper.toProvinceResponse(find(id));
    }

    @Override
    @Transactional
    public ProvinceResponse create(ProvinceRequest request) {
        Province province = new Province();
        province.setSlug(Slugs.forCreate(null, request.name(), provinceRepository::existsBySlug));
        apply(province, request);
        province.setIsActive(request.isActive() == null || request.isActive());
        province.setCreatedBy(author());
        return mapper.toProvinceResponse(provinceRepository.save(province));
    }

    /** A renamed province keeps the address it was published under. */
    @Override
    @Transactional
    public ProvinceResponse update(UUID id, ProvinceRequest request) {
        Province province = find(id);
        apply(province, request);
        if (request.isActive() != null) {
            province.setIsActive(request.isActive());
        }
        return mapper.toProvinceResponse(provinceRepository.saveAndFlush(province));
    }

    /** Refused while destinations are in it (by their service, then by the foreign key). */
    @Override
    @Transactional
    public void delete(UUID id) {
        Province province = find(id);
        events.publishEvent(new ProvinceDeletingEvent(id));
        provinceRepository.delete(province);
        try {
            provinceRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Destinations are still in this province; move them first");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Province getEntity(UUID id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A province is required");
        }
        return provinceRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such province"));
    }

    @EventListener
    public void onRegionDeleting(RegionDeletingEvent event) {
        if (provinceRepository.existsByRegionId(event.regionId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This region still holds provinces; move them to another region first");
        }
    }

    /** A zone is optional and a missing one keeps the current; a tourism region is never a zone. */
    private void apply(Province province, ProvinceRequest request) {
        if (request.regionId() != null) {
            Region zone = regionService.getEntity(request.regionId());
            if (zone.getKind() == RegionKind.TOURISM) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        zone.getName() + " is a tourism region: a province belongs to one of the zones");
            }
            province.setRegion(zone);
        }
        province.setName(request.name().trim());
        province.setCountryCode(request.countryCode() == null || request.countryCode().isBlank()
                ? "KH" : request.countryCode().trim().toUpperCase(Locale.ROOT));
    }

    /** The current account's id, or null for a token with no user (e.g. a service call). */
    private UUID author() {
        try {
            return userService.findUserId(Authorities.current()).orElse(null);
        } catch (ResponseStatusException noIdentity) {
            return null;
        }
    }

    private Province find(UUID id) {
        return provinceRepository.findById(id).orElseThrow(() -> new ProvinceNotFoundException());
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return provinceRepository.count();
    }
}
