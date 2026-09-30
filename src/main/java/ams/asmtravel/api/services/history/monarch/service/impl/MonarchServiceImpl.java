package ams.asmtravel.api.services.history.monarch.service.impl;

import ams.asmtravel.api.services.history.monarch.domain.Monarch;
import ams.asmtravel.api.services.history.monarch.domain.MonarchDeletingEvent;
import ams.asmtravel.api.services.history.monarch.dto.request.MonarchRequest;
import ams.asmtravel.api.services.history.monarch.dto.response.MonarchResponse;
import ams.asmtravel.api.services.history.monarch.exception.MonarchNotFoundException;
import ams.asmtravel.api.services.history.monarch.mapper.MonarchMapper;
import ams.asmtravel.api.services.history.monarch.repository.MonarchRepository;
import ams.asmtravel.api.services.history.monarch.service.MonarchService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonarchServiceImpl implements MonarchService {

    private final MonarchRepository monarchRepository;
    private final MonarchMapper mapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<MonarchResponse> getAll() {
        return monarchRepository.findAllInReignOrder().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MonarchResponse findById(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @Transactional
    public MonarchResponse create(MonarchRequest request) {
        check(request, null);
        Monarch monarch = new Monarch();
        mapper.apply(request, monarch);
        return mapper.toResponse(monarchRepository.save(monarch));
    }

    @Override
    @Transactional
    public MonarchResponse update(UUID id, MonarchRequest request) {
        Monarch monarch = find(id);
        check(request, id);
        mapper.apply(request, monarch);
        return mapper.toResponse(monarchRepository.saveAndFlush(monarch));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Monarch monarch = find(id);
        events.publishEvent(new MonarchDeletingEvent(id));
        monarchRepository.delete(monarch);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, MonarchResponse> findAllById(Collection<UUID> ids) {
        Map<UUID, MonarchResponse> found = new LinkedHashMap<>();
        if (ids != null && !ids.isEmpty()) {
            monarchRepository.findAllById(Set.copyOf(ids)).forEach(monarch -> found.put(monarch.getId(), mapper.toResponse(monarch)));
        }
        return found;
    }

    private void check(MonarchRequest request, UUID id) {
        if (request.reignStartYear() != null && request.reignEndYear() != null
                && request.reignEndYear() < request.reignStartYear()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A reign cannot end before it starts");
        }
        String name = request.name().trim();
        if (id == null ? monarchRepository.existsByNameIgnoreCase(name) : monarchRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another monarch is already called " + name);
        }
    }

    private Monarch find(UUID id) {
        return monarchRepository.findById(id).orElseThrow(() -> new MonarchNotFoundException());
    }
}
