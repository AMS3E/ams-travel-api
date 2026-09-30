package ams.asmtravel.api.services.history.destinationmonarch.service.impl;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.exception.DestinationNotFoundException;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import ams.asmtravel.api.services.history.destinationmonarch.domain.DestinationMonarch;
import ams.asmtravel.api.services.history.destinationmonarch.dto.request.DestinationMonarchRequest;
import ams.asmtravel.api.services.history.destinationmonarch.dto.response.DestinationMonarchResponse;
import ams.asmtravel.api.services.history.destinationmonarch.mapper.DestinationMonarchMapper;
import ams.asmtravel.api.services.history.destinationmonarch.repository.DestinationMonarchRepository;
import ams.asmtravel.api.services.history.destinationmonarch.service.DestinationMonarchService;
import ams.asmtravel.api.services.history.monarch.domain.MonarchDeletingEvent;
import ams.asmtravel.api.services.history.monarch.dto.response.MonarchResponse;
import ams.asmtravel.api.services.history.monarch.service.MonarchService;
import ams.asmtravel.api.services.authorization.domain.Authorities;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DestinationMonarchServiceImpl implements DestinationMonarchService {

    private final DestinationMonarchRepository linkRepository;
    private final DestinationMonarchMapper mapper;
    private final DestinationService destinationService;
    private final MonarchService monarchService;

    /** A place's ties go with it. Runs in the delete's own transaction. */
    @EventListener
    public void onDestinationDeleting(DestinationDeletingEvent event) {
        linkRepository.deleteByDestinationId(event.destinationId());
    }

    /** A deleted monarch is untied from every place. */
    @EventListener
    public void onMonarchDeleting(MonarchDeletingEvent event) {
        linkRepository.deleteByMonarchId(event.monarchId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DestinationMonarchResponse> getAll(UUID destinationId) {
        if (destinationId == null) {
            boolean everything = Authorities.isBackoffice(Authorities.current());
            return respond(linkRepository.findAllWithDestination().stream()
                    .filter(link -> everything || link.getDestination().isPublished())
                    .toList());
        }
        if (!destinationService.exists(destinationId) || !destinationService.canRead(destinationId, Authorities.current())) {
            throw new DestinationNotFoundException();
        }
        return respond(linkRepository.findByDestinations(List.of(destinationId)));
    }

    @Override
    @Transactional
    public List<DestinationMonarchResponse> replace(UUID destinationId, DestinationMonarchRequest request) {
        if (!destinationService.exists(destinationId)) {
            throw new DestinationNotFoundException();
        }
        if (!destinationService.canEdit(destinationId, Authorities.current())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot change this destination's heritage");
        }
        Map<UUID, String> ties = new LinkedHashMap<>();
        request.monarchs().forEach(tie -> ties.putIfAbsent(tie.monarchId(), blankToNull(tie.description())));
        if (monarchService.findAllById(ties.keySet()).size() != ties.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such monarch");
        }

        Destination destination = destinationService.getEntity(destinationId);
        linkRepository.deleteByDestinationId(destinationId);
        linkRepository.flush();
        ties.forEach((monarchId, description) ->
                linkRepository.save(new DestinationMonarch(destination, monarchId, description)));
        linkRepository.flush();
        return respond(linkRepository.findByDestinations(List.of(destinationId)));
    }

    /** Earliest reign first; a tie to a monarch that is gone is left out. */
    private List<DestinationMonarchResponse> respond(List<DestinationMonarch> links) {
        Map<UUID, MonarchResponse> monarchs = monarchService.findAllById(
                links.stream().map(link -> link.getId().getMonarchId()).toList());
        return links.stream()
                .filter(link -> monarchs.containsKey(link.getId().getMonarchId()))
                .map(link -> mapper.toResponse(link, monarchs.get(link.getId().getMonarchId())))
                .sorted(Comparator.comparing(DestinationMonarchResponse::reignStartYear,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
