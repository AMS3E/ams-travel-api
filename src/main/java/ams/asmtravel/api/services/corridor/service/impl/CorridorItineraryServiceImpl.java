package ams.asmtravel.api.services.corridor.service.impl;

import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.corridor.domain.CorridorDay;
import ams.asmtravel.api.services.corridor.domain.CorridorItineraryChangedEvent;
import ams.asmtravel.api.services.corridor.domain.CorridorLeg;
import ams.asmtravel.api.services.corridor.domain.CorridorStatus;
import ams.asmtravel.api.services.corridor.domain.CorridorStop;
import ams.asmtravel.api.services.corridor.domain.RouteSignature;
import ams.asmtravel.api.services.corridor.dto.request.CorridorItineraryRequest;
import ams.asmtravel.api.services.corridor.dto.request.CorridorStopRequest;
import ams.asmtravel.api.services.corridor.dto.response.CorridorItineraryResponse;
import ams.asmtravel.api.services.corridor.dto.response.CorridorStopResponse;
import ams.asmtravel.api.services.corridor.exception.CorridorNotFoundException;
import ams.asmtravel.api.services.corridor.mapper.CorridorItineraryMapper;
import ams.asmtravel.api.services.corridor.repository.CorridorDayRepository;
import ams.asmtravel.api.services.corridor.repository.CorridorRepository;
import ams.asmtravel.api.services.corridor.repository.CorridorStopRepository;
import ams.asmtravel.api.services.corridor.security.CorridorPermissionEvaluator;
import ams.asmtravel.api.services.corridor.service.CorridorItineraryService;
import ams.asmtravel.api.services.corridor.service.CorridorRouteService;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.DestinationDeletingEvent;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import lombok.RequiredArgsConstructor;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CorridorItineraryServiceImpl implements CorridorItineraryService {

    /** Max days in one corridor. */
    private static final int MAX_STOPS = 150;

    private final CorridorRepository corridorRepository;
    private final CorridorDayRepository dayRepository;
    private final CorridorStopRepository stopRepository;
    private final CorridorRouteService routeService;
    private final DestinationService destinationService;
    private final CorridorPermissionEvaluator permission;
    private final CorridorItineraryMapper mapper;
    private final ApplicationEventPublisher events;

    @Override
    @Transactional(readOnly = true)
    public CorridorItineraryResponse getItinerary(UUID corridorId) {
        Corridor corridor = visible(corridorId);
        List<CorridorStop> stops = stopRepository.findInTravelOrder(corridorId);
        return mapper.toResponse(corridor, dayRepository.findByCorridorId(corridorId), stops,
                RouteSignature.status(corridor, stops), routeService.isConfigured());
    }

    @Override
    @Transactional
    public void replaceItinerary(UUID corridorId, CorridorItineraryRequest request) {
        rewrite(find(corridorId), request.days());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CorridorStopResponse> getStops(UUID corridorId) {
        Set<UUID> visible = permission.canCurate(authentication()) ? null
                : corridorRepository.findAllByStatusOrderByIsFeaturedDescNameAsc(CorridorStatus.PUBLISHED).stream()
                .map(Corridor::getId)
                .collect(Collectors.toSet());
        List<CorridorStop> stops = corridorId == null
                ? stopRepository.findAllInTravelOrder()
                : stopRepository.findInTravelOrder(corridorId);
        List<CorridorStopResponse> responses = new ArrayList<>();
        Map<UUID, Integer> sequence = new LinkedHashMap<>();
        for (CorridorStop stop : stops) {
            UUID owner = stop.getDay().getCorridor().getId();
            if (visible == null || visible.contains(owner)) {
                responses.add(mapper.toStopResponse(stop, sequence.merge(owner, 1, Integer::sum)));
            }
        }
        return responses;
    }

    /** Writes one day; stops that stay keep their times and notes. */
    @Override
    @Transactional
    public void replaceStops(UUID corridorId, CorridorStopRequest request) {
        Corridor corridor = find(corridorId);
        List<UUID> destinationIds = request.destinationIds();
        if (new HashSet<>(destinationIds).size() != destinationIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A destination can be on a route only once");
        }
        List<CorridorDay> days = corridor.getDays();
        if (days.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This corridor is planned over " + days.size()
                    + " days; change its stops through PUT /api/v1/corridors/" + corridorId + "/itinerary.");
        }
        CorridorDay day = days.isEmpty() ? null : days.getFirst();
        Map<UUID, CorridorStop> kept = day == null ? Map.of() : day.getStops().stream()
                .collect(Collectors.toMap(stop -> stop.getDestination().getId(), Function.identity(), (first, second) -> first));
        List<CorridorItineraryRequest.Stop> stops = destinationIds.stream()
                .map(id -> kept.containsKey(id) ? asRequested(kept.get(id)) : bareStop(id))
                .toList();
        List<CorridorItineraryRequest.Day> wanted = stops.isEmpty() && day == null ? List.of() : List.of(new CorridorItineraryRequest.Day(
                day == null ? null : day.getTitle(),
                day == null ? null : day.getDescription(),
                day == null ? null : day.getStartTime(),
                day == null ? null : day.getNotes(),
                stops));
        rewrite(corridor, wanted);
    }

    /** On destination delete: removes its stops, renumbers the rest, and signals each corridor changed. */
    @EventListener
    public void onDestinationDeleting(DestinationDeletingEvent event) {
        List<UUID> corridorIds = stopRepository.findCorridorIdsStoppingAt(event.destinationId());
        if (corridorIds.isEmpty()) {
            return;
        }
        List<UUID> dayIds = stopRepository.findDayIdsStoppingAt(event.destinationId());
        stopRepository.deleteByDestinationId(event.destinationId());
        Map<UUID, Integer> nextOrder = new LinkedHashMap<>();
        for (CorridorStop stop : stopRepository.findByDayIds(dayIds)) {
            int order = nextOrder.merge(stop.getDay().getId(), 1, Integer::sum);
            if (!Objects.equals(stop.getStopOrder(), order)) {
                // One at a time, earliest first, to avoid a unique-key clash.
                stop.setStopOrder(order);
                stopRepository.saveAndFlush(stop);
            }
        }
        for (UUID corridorId : corridorIds) {
            corridorRepository.findById(corridorId).ifPresent(corridor -> corridor.summarise(stopRepository.findInTravelOrder(corridorId)));
            events.publishEvent(new CorridorItineraryChangedEvent(corridorId));
        }
    }

    /* Writing. */

    /** Rewrites days and stops in place by position (no renumbering clash) and keeps existing legs. */
    private void rewrite(Corridor corridor, List<CorridorItineraryRequest.Day> days) {
        Map<UUID, Destination> destinations = checked(corridor, days);
        Map<String, CorridorLeg> knownLegs = RouteSignature.legsOf(stopRepository.findInTravelOrder(corridor.getId()));

        List<CorridorDay> dayRows = corridor.getDays();
        for (int index = 0; index < days.size(); index++) {
            CorridorDay day = index < dayRows.size() ? dayRows.get(index) : newDay(corridor);
            writeDay(day, index + 1, days.get(index), destinations);
        }
        trim(dayRows, days.size());

        List<CorridorStop> inOrder = dayRows.stream().flatMap(day -> day.getStops().stream()).toList();
        if (!RouteSignature.restoreLegs(inOrder, knownLegs)) {
            corridor.clearRoute();
        } else if (!RouteSignature.of(inOrder).equals(corridor.getRouteSignature())) {
            // All legs are known, but in a new arrangement: rebuild the route from them.
            corridor.useRoute(inOrder, RouteSignature.of(inOrder), Instant.now());
        }
        corridor.summarise(inOrder);
        corridorRepository.saveAndFlush(corridor);
        events.publishEvent(new CorridorItineraryChangedEvent(corridor.getId()));
    }

    private static CorridorDay newDay(Corridor corridor) {
        CorridorDay day = new CorridorDay();
        day.setCorridor(corridor);
        corridor.getDays().add(day);
        return day;
    }

    private static void writeDay(CorridorDay day, int number, CorridorItineraryRequest.Day wanted, Map<UUID, Destination> destinations) {
        day.setDayNumber(number);
        day.setTitle(blankToNull(wanted.title()));
        day.setDescription(blankToNull(wanted.description()));
        day.setStartTime(wanted.startTime());
        day.setNotes(blankToNull(wanted.notes()));

        List<CorridorStop> stopRows = day.getStops();
        for (int index = 0; index < wanted.stops().size(); index++) {
            CorridorStop stop;
            if (index < stopRows.size()) {
                stop = stopRows.get(index);
            } else {
                stop = new CorridorStop();
                stop.setDay(day);
                stopRows.add(stop);
            }
            writeStop(stop, index + 1, wanted.stops().get(index), destinations);
        }
        trim(stopRows, wanted.stops().size());
    }

    private static void writeStop(CorridorStop stop, int order, CorridorItineraryRequest.Stop wanted, Map<UUID, Destination> destinations) {
        stop.setStopOrder(order);
        stop.setDestination(destinations.get(wanted.destinationId()));
        stop.setArrivalTime(wanted.arrivalTime());
        stop.setDepartureTime(wanted.departureTime());
        stop.setRecommendedDurationMinutes(wanted.recommendedDurationMinutes());
        stop.setTransportType(wanted.transportType());
        stop.setTitle(blankToNull(wanted.title()));
        stop.setDescription(blankToNull(wanted.description()));
        stop.setNotes(blankToNull(wanted.notes()));
    }

    private static <T> void trim(List<T> rows, int size) {
        while (rows.size() > size) {
            rows.removeLast();
        }
    }

    /** Validates the itinerary and returns its destinations by id. */
    private Map<UUID, Destination> checked(Corridor corridor, List<CorridorItineraryRequest.Day> days) {
        if (days.stream().mapToInt(day -> day.stops().size()).sum() > MAX_STOPS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A corridor holds at most " + MAX_STOPS + " stops.");
        }
        Set<UUID> ids = days.stream().flatMap(day -> day.stops().stream())
                .map(CorridorItineraryRequest.Stop::destinationId)
                .collect(Collectors.toSet());
        Map<UUID, Destination> destinations = destinationService.getEntities(ids).stream()
                .collect(Collectors.toMap(Destination::getId, Function.identity()));
        ids.stream().filter(id -> !destinations.containsKey(id)).findFirst().ifPresent(missing -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such destination: " + missing);
        });

        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            List<CorridorItineraryRequest.Stop> stops = days.get(dayIndex).stops();
            for (int stopIndex = 0; stopIndex < stops.size(); stopIndex++) {
                CorridorItineraryRequest.Stop stop = stops.get(stopIndex);
                Destination destination = destinations.get(stop.destinationId());
                String where = "Day " + (dayIndex + 1) + ", stop " + (stopIndex + 1) + " (" + destination.getName() + ")";
                if (stop.arrivalTime() != null && stop.departureTime() != null && stop.departureTime().isBefore(stop.arrivalTime())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, where + " leaves before it arrives.");
                }
                if (corridor.isPublished() && !destination.isPublished()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, where + ": " + destination.getName()
                            + " is not published yet, and a published corridor links every stop to its destination.");
                }
            }
        }
        return destinations;
    }

    /* Helpers. */

    private Corridor find(UUID corridorId) {
        return corridorRepository.findById(corridorId).orElseThrow(() -> new CorridorNotFoundException());
    }

    /** A draft is 404, not 403, for non-curators. */
    private Corridor visible(UUID corridorId) {
        Corridor corridor = find(corridorId);
        if (!permission.canSee(corridor, authentication())) {
            throw new CorridorNotFoundException();
        }
        return corridor;
    }

    private static CorridorItineraryRequest.Stop asRequested(CorridorStop stop) {
        return new CorridorItineraryRequest.Stop(stop.getDestination().getId(), stop.getArrivalTime(), stop.getDepartureTime(),
                stop.getRecommendedDurationMinutes(), stop.getTransportType(), stop.getTitle(), stop.getDescription(), stop.getNotes());
    }

    private static CorridorItineraryRequest.Stop bareStop(UUID destinationId) {
        return new CorridorItineraryRequest.Stop(destinationId, null, null, null, null, null, null, null);
    }

    private static Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
