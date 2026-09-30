package ams.asmtravel.api.services.corridor.service.impl;

import ams.asmtravel.api.services.corridor.client.GoogleRoutesClient;
import ams.asmtravel.api.services.corridor.client.OsmRoutesClient;
import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.corridor.domain.CorridorItineraryChangedEvent;
import ams.asmtravel.api.services.corridor.domain.CorridorLeg;
import ams.asmtravel.api.services.corridor.domain.CorridorPublishedEvent;
import ams.asmtravel.api.services.corridor.domain.CorridorRouteStatus;
import ams.asmtravel.api.services.corridor.domain.CorridorTransportType;
import ams.asmtravel.api.services.corridor.domain.CorridorStop;
import ams.asmtravel.api.services.corridor.domain.RoadRoute;
import ams.asmtravel.api.services.corridor.domain.RoadTravelMode;
import ams.asmtravel.api.services.corridor.domain.RoutePlan;
import ams.asmtravel.api.services.corridor.domain.RouteSignature;
import ams.asmtravel.api.services.corridor.dto.request.CorridorRoutePreviewRequest;
import ams.asmtravel.api.services.corridor.dto.response.CorridorRoutePreviewResponse;
import ams.asmtravel.api.services.corridor.exception.CorridorNotFoundException;
import ams.asmtravel.api.services.corridor.exception.CorridorRouteException;
import ams.asmtravel.api.services.corridor.exception.GoogleRoutesException;
import ams.asmtravel.api.services.corridor.exception.OsmRoutesException;
import ams.asmtravel.api.services.corridor.repository.CorridorRepository;
import ams.asmtravel.api.services.corridor.repository.CorridorStopRepository;
import ams.asmtravel.api.services.corridor.service.CorridorRouteService;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.DestinationRelocatedEvent;
import ams.asmtravel.api.services.destination.destination.service.DestinationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CorridorRouteServiceImpl implements CorridorRouteService {

    private final CorridorRepository corridorRepository;
    private final CorridorStopRepository stopRepository;
    private final GoogleRoutesClient googleRoutes;
    /** Used when no Google key is set. */
    private final OsmRoutesClient osmRoutes;
    private final DestinationService destinationService;
    /* Own transactions, so none stays open during a routing call. */
    private final TransactionTemplate reading;
    private final TransactionTemplate writing;
    /** Corridors being routed now, so one route is never requested twice at once. */
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    public CorridorRouteServiceImpl(CorridorRepository corridorRepository,
                                    CorridorStopRepository stopRepository,
                                    GoogleRoutesClient googleRoutes,
                                    OsmRoutesClient osmRoutes,
                                    DestinationService destinationService,
                                    PlatformTransactionManager transactionManager) {
        this.corridorRepository = corridorRepository;
        this.stopRepository = stopRepository;
        this.googleRoutes = googleRoutes;
        this.osmRoutes = osmRoutes;
        this.destinationService = destinationService;
        this.reading = new TransactionTemplate(transactionManager);
        this.reading.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.reading.setReadOnly(true);
        this.writing = new TransactionTemplate(transactionManager);
        this.writing.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public boolean isConfigured() {
        return googleRoutes.isConfigured() || osmRoutes.isConfigured();
    }

    @Override
    public void calculateRoute(UUID corridorId) {
        calculate(corridorId, false);
    }

    @Override
    public void recalculateRoute(UUID corridorId) {
        calculate(corridorId, true);
    }

    @Override
    public void calculateMissingRoutes() {
        if (!isConfigured()) {
            return;
        }
        // Drafts too, so routes from older leg logic get replaced.
        List<UUID> corridors = reading.execute(status -> corridorRepository
                .findAllByOrderByIsFeaturedDescNameAsc().stream().map(Corridor::getId).toList());
        Objects.requireNonNullElse(corridors, List.<UUID>of()).forEach(this::calculateQuietly);
    }

    @Override
    public CorridorRoutePreviewResponse previewRoute(CorridorRoutePreviewRequest request) {
        List<CorridorRoutePreviewRequest.Stop> wanted = request.stops();
        Set<UUID> ids = wanted.stream().map(CorridorRoutePreviewRequest.Stop::destinationId).collect(Collectors.toSet());
        Map<UUID, Destination> places = Objects.requireNonNullElse(
                reading.execute(status -> destinationService.getEntities(ids).stream()
                        .collect(Collectors.toMap(Destination::getId, Function.identity()))),
                Map.of());
        ids.stream().filter(id -> !places.containsKey(id)).findFirst().ifPresent(missing -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such destination: " + missing);
        });

        // In-memory stops only, so the plan reads them like saved ones.
        List<CorridorStop> stops = new ArrayList<>();
        for (int index = 0; index < wanted.size(); index++) {
            CorridorStop stop = new CorridorStop();
            stop.setDestination(places.get(wanted.get(index).destinationId()));
            stop.setTransportType(index == 0 ? null : CorridorTransportType.orDefault(wanted.get(index).transportType()));
            stops.add(stop);
        }

        CorridorLeg[] legs = new CorridorLeg[stops.size()];
        String error = null;
        if (isConfigured()) {
            // Route each run of pinned stops alone; a stop without a pin empties only its own legs.
            List<RoutePlan.Waypoint> waypoints = RoutePlan.of(stops).waypoints();
            int start = 0;
            for (int end = 0; end <= waypoints.size(); end++) {
                if (end < waypoints.size() && waypoints.get(end).point() != null) {
                    continue;
                }
                if (end < waypoints.size() && error == null) {
                    error = CorridorRouteException.unplaced(waypoints.get(end).name()).getReason();
                }
                if (end - start >= 2) {
                    try {
                        List<CorridorLeg> found = legsFor(new RoutePlan("preview", waypoints.subList(start, end)));
                        for (int index = 1; index < found.size(); index++) {
                            legs[start + index] = found.get(index);
                        }
                    } catch (CorridorRouteException refused) {
                        error = error == null ? refused.getReason() : error;
                    }
                }
                start = end + 1;
            }
        }

        List<CorridorRoutePreviewResponse.Leg> answer = new ArrayList<>();
        for (int index = 1; index < stops.size(); index++) {
            CorridorLeg leg = legs[index];
            answer.add(new CorridorRoutePreviewResponse.Leg(
                    wanted.get(index - 1).destinationId(),
                    wanted.get(index).destinationId(),
                    stops.get(index).getTransportType(),
                    leg == null ? null : leg.distanceKm(),
                    leg == null ? null : leg.minutes(),
                    leg == null ? null : leg.polyline()));
        }
        return new CorridorRoutePreviewResponse(isConfigured(), answer, error);
    }

    /* Triggers. */

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onItineraryChanged(CorridorItineraryChangedEvent event) {
        calculateQuietly(event.corridorId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPublished(CorridorPublishedEvent event) {
        calculateQuietly(event.corridorId());
    }

    /** A destination moved: re-route every corridor that stops there. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDestinationRelocated(DestinationRelocatedEvent event) {
        List<UUID> corridorIds = reading.execute(status -> stopRepository.findCorridorIdsStoppingAt(event.destinationId()));
        Objects.requireNonNullElse(corridorIds, List.<UUID>of()).forEach(this::calculateQuietly);
    }

    /** Routes seeded corridors and any saved while routing was off. */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        calculateMissingRoutes();
    }

    private void calculateQuietly(UUID corridorId) {
        try {
            calculateRoute(corridorId);
        } catch (RuntimeException problem) {
            log.warn("Could not work out the route of corridor {}", corridorId, problem);
        }
    }

    /* Calculation. */

    private void calculate(UUID corridorId, boolean forced) {
        if (!inFlight.add(corridorId)) {
            if (forced) {
                throw CorridorRouteException.busy();
            }
            return;
        }
        try {
            Snapshot snapshot = reading.execute(status -> snapshot(corridorId));
            if (snapshot == null) {
                if (forced) {
                    throw new CorridorNotFoundException();
                }
                return;
            }
            if (!(forced ? calculable(snapshot) : due(snapshot))) {
                return;
            }
            List<CorridorLeg> legs;
            try {
                legs = legsFor(snapshot.plan());
            } catch (CorridorRouteException problem) {
                writing.executeWithoutResult(status -> corridorRepository.findById(corridorId)
                        .ifPresent(corridor -> corridor.routeFailed(problem.getReason(), Instant.now())));
                if (forced) {
                    throw problem;
                }
                log.warn("Route for corridor {} not worked out: {}{}", corridorId, problem.getReason(),
                        problem.getCause() == null ? "" : " (" + problem.getCause().getMessage() + ")");
                return;
            }
            writing.executeWithoutResult(status -> store(corridorId, snapshot.plan().signature(), legs));
        } finally {
            inFlight.remove(corridorId);
        }
    }

    private Snapshot snapshot(UUID corridorId) {
        return corridorRepository.findById(corridorId).map(corridor -> {
            List<CorridorStop> stops = stopRepository.findInTravelOrder(corridorId);
            return new Snapshot(RoutePlan.of(stops), RouteSignature.status(corridor, stops));
        }).orElse(null);
    }

    /** Automatic run: only if routing is configured and the route is missing or stale. */
    private boolean due(Snapshot snapshot) {
        return isConfigured()
                && (snapshot.status() == CorridorRouteStatus.MISSING || snapshot.status() == CorridorRouteStatus.STALE);
    }

    /** Forced run: always, unless it is impossible. */
    private boolean calculable(Snapshot snapshot) {
        if (snapshot.plan().waypoints().size() < 2) {
            throw CorridorRouteException.tooFewStops();
        }
        if (!isConfigured()) {
            throw CorridorRouteException.notConfigured();
        }
        return true;
    }

    /** One leg per stop (null for the first); road runs are routed, the rest measured here. */
    private List<CorridorLeg> legsFor(RoutePlan plan) {
        String unplaced = plan.firstUnplaced();
        if (unplaced != null) {
            throw CorridorRouteException.unplaced(unplaced);
        }
        CorridorLeg[] legs = new CorridorLeg[plan.waypoints().size()];
        for (RoutePlan.Segment segment : plan.segments(GoogleRoutesClient.MAX_INTERMEDIATES)) {
            switch (segment) {
                case RoutePlan.OffRoad leg ->
                        legs[leg.stop()] = CorridorLeg.offRoad(leg.from().point(), leg.to().point(), leg.to().transport());
                case RoutePlan.RoadRun run -> {
                    RoadAnswer answer = askForRoads(run);
                    for (int index = 0; index < run.points().size() - 1; index++) {
                        RoutePlan.Waypoint from = run.points().get(index);
                        RoutePlan.Waypoint arrival = run.points().get(index + 1);
                        legs[run.firstStop() + index] = answer == null
                                ? CorridorLeg.offRoad(from.point(), arrival.point(), arrival.transport())
                                : CorridorLeg.road(answer.route().legs().get(index), arrival.transport(), answer.fellBack());
                    }
                }
            }
        }
        return Arrays.asList(legs);
    }

    /** Falls back to car roads when the mode has no route; null means draw a boat leg straight. */
    private RoadAnswer askForRoads(RoutePlan.RoadRun run) {
        RoadRoute route = computeRoute(run, run.mode());
        boolean fellBack = false;
        if (route == null && run.mode() != RoadTravelMode.DRIVE) {
            route = computeRoute(run, RoadTravelMode.DRIVE);
            fellBack = true;
        }
        if (route == null && run.ownPace()) {
            return null;
        }
        if (route == null) {
            throw CorridorRouteException.noRoad(run.points().getFirst().name(), run.points().getLast().name());
        }
        if (route.legs().size() != run.points().size() - 1) {
            throw CorridorRouteException.unexpectedLegs(route.legs().size(), run.points().size() - 1);
        }
        return new RoadAnswer(route, fellBack);
    }

    /** Google if keyed, otherwise OSM. */
    private RoadRoute computeRoute(RoutePlan.RoadRun run, RoadTravelMode mode) {
        if (googleRoutes.isConfigured()) {
            try {
                return googleRoutes.computeRoute(run.positions(), mode).orElse(null);
            } catch (GoogleRoutesException problem) {
                throw googleRoutes.isConfigured() ? CorridorRouteException.from(problem) : CorridorRouteException.notConfigured();
            }
        }
        if (osmRoutes.isConfigured()) {
            try {
                return osmRoutes.computeRoute(run.positions(), mode).orElse(null);
            } catch (OsmRoutesException problem) {
                throw CorridorRouteException.from(problem);
            }
        }
        throw CorridorRouteException.notConfigured();
    }

    /** Saves only if the stops did not change during the calculation. */
    private void store(UUID corridorId, String signature, List<CorridorLeg> legs) {
        Corridor corridor = corridorRepository.findById(corridorId).orElse(null);
        List<CorridorStop> stops = stopRepository.findInTravelOrder(corridorId);
        if (corridor == null || !RouteSignature.of(stops).equals(signature)) {
            log.info("Stops of corridor {} changed while its route was worked out; keeping the newer request", corridorId);
            return;
        }
        for (int index = 0; index < stops.size(); index++) {
            stops.get(index).setLeg(legs.get(index));
        }
        corridor.useRoute(stops, signature, Instant.now());
        log.info("Route for corridor {} worked out: {} km over {} stops", corridorId, corridor.getTotalDistanceKm(), stops.size());
    }

    private record Snapshot(RoutePlan plan, CorridorRouteStatus status) {
    }

    private record RoadAnswer(RoadRoute route, boolean fellBack) {
    }
}
