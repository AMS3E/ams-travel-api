package ams.asmtravel.api.services.history.architecture.service.impl;

import ams.asmtravel.api.services.history.architecture.domain.ArchitectureStyle;
import ams.asmtravel.api.services.history.architecture.domain.ArchitectureStyleDeletingEvent;
import ams.asmtravel.api.services.history.architecture.dto.request.ArchitectureStyleRequest;
import ams.asmtravel.api.services.history.architecture.dto.response.ArchitectureStyleResponse;
import ams.asmtravel.api.services.history.architecture.exception.ArchitectureStyleNotFoundException;
import ams.asmtravel.api.services.history.architecture.mapper.ArchitectureStyleMapper;
import ams.asmtravel.api.services.history.architecture.repository.ArchitectureStyleRepository;
import ams.asmtravel.api.services.history.architecture.service.ArchitectureStyleService;
import ams.asmtravel.api.services.history.period.domain.HistoricalPeriodDeletingEvent;
import ams.asmtravel.api.services.history.period.service.HistoricalPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
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
public class ArchitectureStyleServiceImpl implements ArchitectureStyleService {

    private final ArchitectureStyleRepository styleRepository;
    private final HistoricalPeriodService periodService;
    private final ArchitectureStyleMapper mapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public List<ArchitectureStyleResponse> getAll(UUID periodId) {
        List<ArchitectureStyle> styles = periodId == null
                ? styleRepository.findAllByOrderByNameAsc()
                : styleRepository.findAllByPeriodIdOrderByNameAsc(periodId);
        return styles.stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ArchitectureStyleResponse findById(UUID id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @Transactional
    public ArchitectureStyleResponse create(ArchitectureStyleRequest request) {
        check(request, null);
        ArchitectureStyle style = new ArchitectureStyle();
        mapper.apply(request, style);
        return mapper.toResponse(styleRepository.save(style));
    }

    @Override
    @Transactional
    public ArchitectureStyleResponse update(UUID id, ArchitectureStyleRequest request) {
        ArchitectureStyle style = find(id);
        check(request, id);
        mapper.apply(request, style);
        return mapper.toResponse(styleRepository.saveAndFlush(style));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        ArchitectureStyle style = find(id);
        events.publishEvent(new ArchitectureStyleDeletingEvent(id));
        styleRepository.delete(style);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ArchitectureStyleResponse> findAllById(Collection<UUID> ids) {
        Map<UUID, ArchitectureStyleResponse> found = new LinkedHashMap<>();
        if (ids != null && !ids.isEmpty()) {
            styleRepository.findAllById(Set.copyOf(ids)).forEach(style -> found.put(style.getId(), mapper.toResponse(style)));
        }
        return found;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<ArchitectureStyle> getEntities(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<UUID> wanted = new HashSet<>(ids);
        Set<ArchitectureStyle> found = new LinkedHashSet<>(styleRepository.findAllById(wanted));
        if (found.size() != wanted.size()) {
            found.stream().map(ArchitectureStyle::getId).forEach(wanted::remove);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such architecture style: " + wanted.iterator().next());
        }
        return found;
    }

    /** A deleted period's styles stay, in no period. Runs in the delete's own transaction. */
    @EventListener
    public void onPeriodDeleting(HistoricalPeriodDeletingEvent event) {
        styleRepository.clearPeriod(event.periodId());
    }

    private void check(ArchitectureStyleRequest request, UUID id) {
        if (request.periodId() != null && !periodService.exists(request.periodId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such historical period");
        }
        String name = request.name().trim();
        if (id == null ? styleRepository.existsByNameIgnoreCase(name) : styleRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another style is already called " + name);
        }
    }

    private ArchitectureStyle find(UUID id) {
        return styleRepository.findById(id).orElseThrow(() -> new ArchitectureStyleNotFoundException());
    }
}
