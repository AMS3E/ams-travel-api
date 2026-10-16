package ams.asmtravel.api.services.corridor.mapper;

import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.corridor.domain.CorridorDay;
import ams.asmtravel.api.services.corridor.domain.CorridorRouteStatus;
import ams.asmtravel.api.services.corridor.domain.CorridorStop;
import ams.asmtravel.api.services.corridor.dto.response.CorridorItineraryResponse;
import ams.asmtravel.api.services.corridor.dto.response.CorridorStopResponse;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.media.service.MediaService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Maps a corridor's itinerary. Road data is sent only when the route is CURRENT (a stale line draws wrong). */
@Mapper(componentModel = "spring")
public abstract class CorridorItineraryMapper {

    @Autowired
    protected MediaService mediaService;

    @Autowired
    protected CorridorMapper corridorMapper;

    public CorridorItineraryResponse toResponse(Corridor corridor, List<CorridorDay> days, List<CorridorStop> stopsInOrder,
                                                CorridorRouteStatus routeStatus, boolean routesConfigured) {
        boolean current = routeStatus == CorridorRouteStatus.CURRENT;
        Map<UUID, List<CorridorStop>> stopsByDay = stopsInOrder.stream()
                .collect(Collectors.groupingBy(stop -> stop.getDay().getId()));
        List<CorridorItineraryResponse.Day> dayResponses = new ArrayList<>();
        int sequence = 0;
        for (CorridorDay day : days) {
            List<CorridorItineraryResponse.Stop> stops = new ArrayList<>();
            for (CorridorStop stop : stopsByDay.getOrDefault(day.getId(), List.of())) {
                stops.add(toStop(stop, ++sequence, current));
            }
            dayResponses.add(toDay(day, stops));
        }
        CorridorItineraryResponse.Route route = new CorridorItineraryResponse.Route(
                routeStatus,
                current ? corridor.getEncodedPolyline() : null,
                current ? corridor.getTotalDistanceKm() : null,
                current ? corridor.getTotalDurationMinutes() : null,
                corridor.getRouteCalculatedAt(),
                corridor.getRouteError(),
                routesConfigured);
        return new CorridorItineraryResponse(corridorMapper.toResponse(corridor, stopsInOrder, routeStatus), dayResponses, route);
    }

    /** A stop for {@code /corridor-destinations}; {@code sortOrder} is over the whole corridor. */
    @Mapping(target = "corridorId", source = "stop.day.corridor.id")
    @Mapping(target = "destinationId", source = "stop.destination.id")
    @Mapping(target = "sortOrder", source = "sequence")
    public abstract CorridorStopResponse toStopResponse(CorridorStop stop, Integer sequence);

    /** Day travel = legs arriving at its stops, including the one from the previous day. */
    private CorridorItineraryResponse.Day toDay(CorridorDay day, List<CorridorItineraryResponse.Stop> stops) {
        List<CorridorItineraryResponse.Stop> travelled = stops.stream().filter(stop -> stop.distanceFromPreviousKm() != null).toList();
        BigDecimal distance = travelled.isEmpty() ? null
                : travelled.stream().map(CorridorItineraryResponse.Stop::distanceFromPreviousKm).reduce(BigDecimal.ZERO, BigDecimal::add);
        Integer minutes = travelled.isEmpty() ? null
                : travelled.stream().mapToInt(stop -> Objects.requireNonNullElse(stop.travelTimeFromPreviousMinutes(), 0)).sum();
        return new CorridorItineraryResponse.Day(day.getId(), day.getDayNumber(), day.getTitle(), day.getDescription(),
                day.getStartTime(), day.getNotes(), distance, minutes, stops);
    }

    private CorridorItineraryResponse.Stop toStop(CorridorStop stop, int sequence, boolean routeCurrent) {
        return new CorridorItineraryResponse.Stop(
                stop.getId(),
                sequence,
                stop.getStopOrder(),
                toDestination(stop.getDestination()),
                stop.getArrivalTime(),
                stop.getDepartureTime(),
                stop.getRecommendedDurationMinutes(),
                sequence == 1 ? null : stop.getTransportType(),
                routeCurrent ? stop.getDistanceFromPreviousKm() : null,
                routeCurrent ? stop.getTravelTimeFromPreviousMinutes() : null,
                routeCurrent ? stop.getRoutePolyline() : null,
                stop.getTitle(),
                stop.getDescription(),
                stop.getNotes());
    }

    private CorridorItineraryResponse.Destination toDestination(Destination destination) {
        return new CorridorItineraryResponse.Destination(
                destination.getId(),
                destination.getName(),
                destination.getSlug(),
                destination.getStatus(),
                destination.getShortDescription(),
                mediaService.url(destination.getCoverKey()),
                destination.getProvince().getId(),
                destination.getProvince().getName(),
                destination.getProvince().getRegion() == null ? null : destination.getProvince().getRegion().getId(),
                destination.getLatitude(),
                destination.getLongitude());
    }
}
