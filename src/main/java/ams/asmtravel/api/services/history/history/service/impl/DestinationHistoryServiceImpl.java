package ams.asmtravel.api.services.history.history.service.impl;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.exception.DestinationNotFoundException;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.history.history.domain.DestinationHistory;
import ams.asmtravel.api.services.history.history.dto.request.DestinationHistoryRequest;
import ams.asmtravel.api.services.history.history.dto.response.DestinationHistoryResponse;
import ams.asmtravel.api.services.history.history.exception.DestinationHistoryNotFoundException;
import ams.asmtravel.api.services.history.history.mapper.HistoryMapper;
import ams.asmtravel.api.services.history.history.repository.DestinationHistoryRepository;
import ams.asmtravel.api.services.history.history.service.DestinationHistoryService;
import ams.asmtravel.api.services.history.period.domain.HistoricalPeriodDeletingEvent;
import ams.asmtravel.api.services.history.period.dto.response.HistoricalPeriodResponse;
import ams.asmtravel.api.services.history.period.service.HistoricalPeriodService;
import ams.asmtravel.api.services.authorization.domain.Authorities;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DestinationHistoryServiceImpl implements DestinationHistoryService {

    private final DestinationHistoryRepository historyRepository;
    private final DestinationService destinationService;
    private final HistoricalPeriodService periodService;
    private final HistoryMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<DestinationHistoryResponse> getTimeline(UUID destinationId) {
        readable(destinationId);
        List<DestinationHistory> timeline = historyRepository.findTimelines(List.of(destinationId));
        Map<UUID, HistoricalPeriodResponse> periods = periodService.findAllById(
                timeline.stream().map(DestinationHistory::getHistoricalPeriodId).filter(Objects::nonNull).toList());
        return timeline.stream().map(moment -> respond(moment, periods)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DestinationHistoryResponse> getAll() {
        boolean everything = Authorities.isBackoffice(Authorities.current());
        List<DestinationHistory> timeline = historyRepository.findAllInOrder().stream()
                .filter(moment -> everything || moment.getDestination().isPublished())
                .toList();
        Map<UUID, HistoricalPeriodResponse> periods = periodService.findAllById(
                timeline.stream().map(DestinationHistory::getHistoricalPeriodId).filter(Objects::nonNull).toList());
        return timeline.stream().map(moment -> respond(moment, periods)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DestinationHistoryResponse findById(UUID id) {
        DestinationHistory moment = find(id);
        if (!destinationService.canRead(moment.getDestination().getId(), Authorities.current())) {
            throw new DestinationHistoryNotFoundException();
        }
        return respond(moment);
    }

    @Override
    @Transactional
    public DestinationHistoryResponse create(DestinationHistoryRequest request) {
        DestinationHistory moment = new DestinationHistory();
        moment.setDestination(editable(request.destinationId()));
        apply(moment, request);
        return respond(historyRepository.save(moment));
    }

    @Override
    @Transactional
    public DestinationHistoryResponse update(UUID id, DestinationHistoryRequest request) {
        DestinationHistory moment = find(id);
        editable(moment.getDestination().getId());
        if (!moment.getDestination().getId().equals(request.destinationId())) {
            moment.setDestination(editable(request.destinationId()));
        }
        apply(moment, request);
        return respond(historyRepository.saveAndFlush(moment));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        DestinationHistory moment = find(id);
        editable(moment.getDestination().getId());
        historyRepository.delete(moment);
    }

    /** A place's timeline goes with it. Runs in the delete's own transaction. */
    @EventListener
    public void onDestinationDeleting(DestinationDeletingEvent event) {
        historyRepository.deleteByDestinationId(event.destinationId());
    }

    /** A deleted period's moments stay, in no period. */
    @EventListener
    public void onPeriodDeleting(HistoricalPeriodDeletingEvent event) {
        historyRepository.clearPeriod(event.periodId());
    }

    private void apply(DestinationHistory moment, DestinationHistoryRequest request) {
        if (request.historicalPeriodId() != null && !periodService.exists(request.historicalPeriodId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such historical period");
        }
        if (request.startDate() != null && request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A timeline entry cannot end before it starts");
        }
        mapper.apply(request, moment);
    }

    private DestinationHistoryResponse respond(DestinationHistory moment) {
        UUID periodId = moment.getHistoricalPeriodId();
        return respond(moment, periodId == null ? Map.of() : periodService.findAllById(List.of(periodId)));
    }

    private DestinationHistoryResponse respond(DestinationHistory moment, Map<UUID, HistoricalPeriodResponse> periods) {
        HistoricalPeriodResponse period = moment.getHistoricalPeriodId() == null ? null : periods.get(moment.getHistoricalPeriodId());
        return mapper.toResponse(moment, period == null ? null : period.name());
    }

    private void readable(UUID destinationId) {
        if (!destinationService.exists(destinationId) || !destinationService.canRead(destinationId, Authorities.current())) {
            throw new DestinationNotFoundException();
        }
    }

    private Destination editable(UUID destinationId) {
        if (!destinationService.exists(destinationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such destination");
        }
        if (!destinationService.canEdit(destinationId, Authorities.current())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot change this destination's history");
        }
        return destinationService.getEntity(destinationId);
    }

    private DestinationHistory find(UUID id) {
        return historyRepository.findById(id).orElseThrow(() -> new DestinationHistoryNotFoundException());
    }
}
