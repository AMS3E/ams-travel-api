package ams.asmtravel.api.services.history.religion.service.impl;

import ams.asmtravel.api.services.history.religion.domain.Religion;
import ams.asmtravel.api.services.history.religion.domain.ReligionDeletingEvent;
import ams.asmtravel.api.services.history.religion.dto.request.ReligionRequest;
import ams.asmtravel.api.services.history.religion.dto.response.ReligionResponse;
import ams.asmtravel.api.services.history.religion.exception.ReligionNotFoundException;
import ams.asmtravel.api.services.history.religion.mapper.ReligionMapper;
import ams.asmtravel.api.services.history.religion.repository.ReligionRepository;
import ams.asmtravel.api.services.history.religion.service.ReligionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReligionServiceImpl implements ReligionService {

    private final ReligionRepository religionRepository;
    private final ReligionMapper mapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<ReligionResponse> getAll() {
        return religionRepository.findAllByOrderByNameAsc().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReligionResponse findById(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @Transactional
    public ReligionResponse create(ReligionRequest request) {
        checkName(request.name().trim(), null);
        Religion religion = new Religion();
        mapper.apply(request, religion);
        return mapper.toResponse(religionRepository.save(religion));
    }

    @Override
    @Transactional
    public ReligionResponse update(UUID id, ReligionRequest request) {
        Religion religion = find(id);
        checkName(request.name().trim(), id);
        mapper.apply(request, religion);
        return mapper.toResponse(religionRepository.saveAndFlush(religion));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Religion religion = find(id);
        events.publishEvent(new ReligionDeletingEvent(id));
        religionRepository.delete(religion);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ReligionResponse> findAllById(Collection<UUID> ids) {
        Map<UUID, ReligionResponse> found = new LinkedHashMap<>();
        if (ids != null && !ids.isEmpty()) {
            religionRepository.findAllById(Set.copyOf(ids)).forEach(religion -> found.put(religion.getId(), mapper.toResponse(religion)));
        }
        return found;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Religion> getEntities(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<UUID> wanted = new HashSet<>(ids);
        Set<Religion> found = new LinkedHashSet<>(religionRepository.findAllById(wanted));
        if (found.size() != wanted.size()) {
            found.stream().map(Religion::getId).forEach(wanted::remove);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such religion: " + wanted.iterator().next());
        }
        return found;
    }

    private void checkName(String name, UUID id) {
        if (id == null ? religionRepository.existsByNameIgnoreCase(name) : religionRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another religion is already called " + name);
        }
    }

    private Religion find(UUID id) {
        return religionRepository.findById(id).orElseThrow(() -> new ReligionNotFoundException());
    }
}
