package ams.asmtravel.api.services.history.period.service.impl;

import ams.asmtravel.api.services.history.period.domain.HistoricalPeriod;
import ams.asmtravel.api.services.history.period.domain.HistoricalPeriodDeletingEvent;
import ams.asmtravel.api.services.history.period.dto.request.HistoricalPeriodRequest;
import ams.asmtravel.api.services.history.period.dto.response.HistoricalPeriodResponse;
import ams.asmtravel.api.services.history.period.exception.HistoricalPeriodNotFoundException;
import ams.asmtravel.api.services.history.period.mapper.HistoricalPeriodMapper;
import ams.asmtravel.api.services.history.period.repository.HistoricalPeriodRepository;
import ams.asmtravel.api.services.history.period.service.HistoricalPeriodService;
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
public class HistoricalPeriodServiceImpl implements HistoricalPeriodService {

    private final HistoricalPeriodRepository periodRepository;
    private final HistoricalPeriodMapper mapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<HistoricalPeriodResponse> getAll() {
        return periodRepository.findAllOldestFirst().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public HistoricalPeriodResponse findById(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @Transactional
    public HistoricalPeriodResponse create(HistoricalPeriodRequest request) {
        check(request, null);
        HistoricalPeriod period = new HistoricalPeriod();
        mapper.apply(request, period);
        return mapper.toResponse(periodRepository.save(period));
    }

    @Override
    @Transactional
    public HistoricalPeriodResponse update(UUID id, HistoricalPeriodRequest request) {
        HistoricalPeriod period = find(id);
        check(request, id);
        mapper.apply(request, period);
        return mapper.toResponse(periodRepository.saveAndFlush(period));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        HistoricalPeriod period = find(id);
        events.publishEvent(new HistoricalPeriodDeletingEvent(id));
        periodRepository.delete(period);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return id != null && periodRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, HistoricalPeriodResponse> findAllById(Collection<UUID> ids) {
        Map<UUID, HistoricalPeriodResponse> found = new LinkedHashMap<>();
        if (ids != null && !ids.isEmpty()) {
            periodRepository.findAllById(Set.copyOf(ids)).forEach(period -> found.put(period.getId(), mapper.toResponse(period)));
        }
        return found;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<HistoricalPeriod> getEntities(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<UUID> wanted = new HashSet<>(ids);
        Set<HistoricalPeriod> found = new LinkedHashSet<>(periodRepository.findAllById(wanted));
        if (found.size() != wanted.size()) {
            found.stream().map(HistoricalPeriod::getId).forEach(wanted::remove);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such historical period: " + wanted.iterator().next());
        }
        return found;
    }

    private void check(HistoricalPeriodRequest request, UUID id) {
        if (request.startYear() != null && request.endYear() != null && request.endYear() < request.startYear()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A period cannot end before it starts");
        }
        String name = request.name().trim();
        if (id == null ? periodRepository.existsByNameIgnoreCase(name) : periodRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another period is already called " + name);
        }
    }

    private HistoricalPeriod find(UUID id) {
        return periodRepository.findById(id).orElseThrow(() -> new HistoricalPeriodNotFoundException());
    }
}
