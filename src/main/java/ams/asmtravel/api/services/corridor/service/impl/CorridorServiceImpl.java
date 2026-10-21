package ams.asmtravel.api.services.corridor.service.impl;

import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.corridor.domain.CorridorDeletingEvent;
import ams.asmtravel.api.services.corridor.domain.CorridorPublishedEvent;
import ams.asmtravel.api.services.corridor.domain.CorridorStatus;
import ams.asmtravel.api.services.corridor.domain.CorridorStop;
import ams.asmtravel.api.services.corridor.domain.RouteSignature;
import ams.asmtravel.api.services.corridor.dto.request.CorridorRequest;
import ams.asmtravel.api.services.corridor.dto.response.CorridorResponse;
import ams.asmtravel.api.services.corridor.exception.CorridorNotFoundException;
import ams.asmtravel.api.services.corridor.mapper.CorridorMapper;
import ams.asmtravel.api.services.corridor.repository.CorridorRepository;
import ams.asmtravel.api.services.corridor.repository.CorridorStopRepository;
import ams.asmtravel.api.services.corridor.security.CorridorPermissionEvaluator;
import ams.asmtravel.api.services.corridor.service.CorridorService;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.Slugs;
import ams.asmtravel.api.services.taxonomy.interest.domain.InterestDeletingEvent;
import ams.asmtravel.api.services.taxonomy.interest.service.InterestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CorridorServiceImpl implements CorridorService {

    private final CorridorRepository corridorRepository;
    private final CorridorStopRepository stopRepository;
    private final CorridorPermissionEvaluator permission;
    private final CorridorMapper mapper;
    private final InterestService interestService;
    private final ApplicationEventPublisher events;

    /** By sort order; unnumbered ones after, then by name. */
    private static final Comparator<Corridor> IN_ORDER = Comparator
            .comparing(Corridor::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Corridor::getName, String.CASE_INSENSITIVE_ORDER);

    @Override
    @Transactional(readOnly = true)
    public List<CorridorResponse> getAll(String slug) {
        boolean curator = permission.canCurate(SecurityContextHolder.getContext().getAuthentication());
        List<Corridor> corridors;
        if (slug != null && !slug.isBlank()) {
            corridors = corridorRepository.findBySlug(slug.trim())
                    .filter(corridor -> curator || corridor.isPublished())
                    .stream().toList();
        } else {
            corridors = curator
                    ? corridorRepository.findAllByOrderByNameAsc()
                    : corridorRepository.findAllByStatusOrderByNameAsc(CorridorStatus.PUBLISHED);
        }
        Map<UUID, List<CorridorStop>> stops = stopRepository.findAllInTravelOrder().stream()
                .collect(Collectors.groupingBy(stop -> stop.getDay().getCorridor().getId(), LinkedHashMap::new, Collectors.toList()));
        return corridors.stream()
                .sorted(IN_ORDER)
                .map(corridor -> response(corridor, stops.getOrDefault(corridor.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CorridorResponse findById(UUID id) {
        Corridor corridor = getEntity(id);
        if (!permission.canSee(corridor, SecurityContextHolder.getContext().getAuthentication())) {
            throw new CorridorNotFoundException();
        }
        return response(corridor, stopRepository.findInTravelOrder(id));
    }

    @Override
    @Transactional
    public CorridorResponse create(CorridorRequest request, UUID authorId) {
        List<Corridor> others = listed(null);
        Corridor corridor = new Corridor();
        corridor.setSlug(Slugs.forCreate(request.slug(), request.name(), corridorRepository::existsBySlug));
        apply(corridor, request);
        linkInterests(corridor, request);
        corridor.setStatus(CorridorStatus.DRAFT);
        corridor.setCreatedBy(authorId);
        placeIn(corridor, others, request.sortOrder());
        corridor = corridorRepository.save(corridor);
        log.info("Corridor {} ({}) drafted by {}", corridor.getName(), corridor.getId(), authorId);
        return response(corridor, List.of());
    }

    @Override
    @Transactional
    public CorridorResponse update(UUID id, CorridorRequest request) {
        Corridor corridor = getEntity(id);
        List<Corridor> others = listed(id);
        corridor.setSlug(Slugs.forUpdate(request.slug(), corridor.getSlug(),
                slug -> corridorRepository.existsBySlugAndIdNot(slug, id)));
        apply(corridor, request);
        linkInterests(corridor, request);
        placeIn(corridor, others, request.sortOrder() != null ? request.sortOrder() : corridor.getSortOrder());
        return response(corridorRepository.saveAndFlush(corridor), stopRepository.findInTravelOrder(id));
    }

    /** Goes live even without stops; every stop must be a published destination (the site links it). */
    @Override
    @Transactional
    public void publish(UUID id) {
        Corridor corridor = getEntity(id);
        List<String> drafts = stopRepository.findInTravelOrder(id).stream()
                .map(CorridorStop::getDestination)
                .filter(destination -> !destination.isPublished())
                .map(Destination::getName)
                .distinct()
                .toList();
        if (!drafts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Publish " + String.join(", ", drafts) + " first: the public site links every stop to its destination.");
        }
        corridor.setStatus(CorridorStatus.PUBLISHED);
        corridor.setPublishedAt(Instant.now());
        events.publishEvent(new CorridorPublishedEvent(id));
    }

    @Override
    @Transactional
    public void unpublish(UUID id) {
        getEntity(id).setStatus(CorridorStatus.DRAFT);
    }

    /** Days and stops go by cascade; favorites go via the event. */
    @Override
    @Transactional
    public void delete(UUID id) {
        Corridor corridor = getEntity(id);
        events.publishEvent(new CorridorDeletingEvent(id));
        corridorRepository.delete(corridor);
        List<Corridor> left = listed(id);
        for (int index = 0; index < left.size(); index++) {
            left.get(index).setSortOrder(index);
        }
        log.info("Corridor {} ({}) deleted", corridor.getName(), id);
    }

    @Override
    @Transactional(readOnly = true)
    public Corridor getEntity(UUID id) {
        return corridorRepository.findById(id).orElseThrow(() -> new CorridorNotFoundException());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return id != null && corridorRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPublished(UUID id) {
        return id != null && corridorRepository.findById(id).map(Corridor::isPublished).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return corridorRepository.count();
    }

    /** A curator may change any corridor's photos; nobody else may. */
    @Override
    @Transactional(readOnly = true)
    public boolean canWrite(UUID id, Authentication authentication) {
        return exists(id) && permission.canCurate(authentication);
    }

    /** Set from its gallery when a photo is made the cover, or cleared when that photo goes. */
    @Override
    @Transactional
    public void setCover(UUID id, String coverKey) {
        Corridor corridor = getEntity(id);
        corridor.setCoverKey(coverKey);
        corridorRepository.save(corridor);
    }

    private CorridorResponse response(Corridor corridor, List<CorridorStop> stopsInOrder) {
        return mapper.toResponse(corridor, stopsInOrder, RouteSignature.status(corridor, stopsInOrder));
    }

    /** The ordered corridors without one; read before any change. */
    private List<Corridor> listed(UUID without) {
        return new ArrayList<>(corridorRepository.findAll().stream()
                .filter(other -> !other.getId().equals(without))
                .sorted(IN_ORDER)
                .toList());
    }

    /** Inserts the corridor at its place (end if none) and renumbers all from 0. */
    private static void placeIn(Corridor corridor, List<Corridor> others, Integer place) {
        int at = place == null ? others.size() : Math.max(0, Math.min(place, others.size()));
        others.add(at, corridor);
        for (int index = 0; index < others.size(); index++) {
            others.get(index).setSortOrder(index);
        }
    }

    /** Unlinks corridors from a deleted interest. */
    @EventListener
    public void onInterestDeleting(InterestDeletingEvent event) {
        corridorRepository.unlinkInterest(event.interestId());
    }

    /** Null keeps an existing corridor's interests unchanged. */
    private void linkInterests(Corridor corridor, CorridorRequest request) {
        if (request.interestIds() != null || corridor.getId() == null) {
            corridor.setInterests(new HashSet<>(interestService.getEntities(
                    request.interestIds() == null ? Set.of() : request.interestIds())));
        }
    }

    private static void apply(Corridor corridor, CorridorRequest request) {
        if (request.estimatedBudgetMin() != null && request.estimatedBudgetMax() != null
                && request.estimatedBudgetMax().compareTo(request.estimatedBudgetMin()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The budget's upper end is below its lower end.");
        }
        corridor.setName(request.name().trim());
        corridor.setShortDescription(blankToNull(request.shortDescription()));
        corridor.setDescription(blankToNull(request.description()));
        if (request.nameKm() != null) {
            corridor.setNameKm(blankToNull(request.nameKm()));
        }
        if (request.shortDescriptionKm() != null) {
            corridor.setShortDescriptionKm(blankToNull(request.shortDescriptionKm()));
        }
        if (request.descriptionKm() != null) {
            corridor.setDescriptionKm(blankToNull(request.descriptionKm()));
        }
        corridor.setCoverKey(blankToNull(request.coverKey()));
        corridor.setEstimatedBudgetMin(request.estimatedBudgetMin());
        corridor.setEstimatedBudgetMax(request.estimatedBudgetMax());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
