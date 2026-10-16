package ams.asmtravel.api.services.corridor.mapper;

import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.corridor.domain.CorridorRouteStatus;
import ams.asmtravel.api.services.corridor.domain.CorridorStop;
import ams.asmtravel.api.services.corridor.dto.response.CorridorResponse;
import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.location.region.domain.Region;
import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class CorridorMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "id", source = "corridor.id")
    @Mapping(target = "coverUrl", expression = "java(mediaService.url(corridor.getCoverKey()))")
    @Mapping(target = "isActive", expression = "java(corridor.isPublished())")
    @Mapping(target = "interestIds", expression = "java(interestIds(corridor))")
    @Mapping(target = "stopCount", expression = "java(stopsInOrder.size())")
    @Mapping(target = "stopNames", expression = "java(stopNames(stopsInOrder))")
    @Mapping(target = "stopDestinationIds", expression = "java(stopDestinationIds(stopsInOrder))")
    @Mapping(target = "stopProvinceIds", expression = "java(stopProvinceIds(stopsInOrder))")
    @Mapping(target = "stopRegionIds", expression = "java(stopRegionIds(stopsInOrder))")
    @Mapping(target = "routeStatus", source = "routeStatus")
    public abstract CorridorResponse toResponse(Corridor corridor, List<CorridorStop> stopsInOrder, CorridorRouteStatus routeStatus);

    protected List<UUID> interestIds(Corridor corridor) {
        return corridor.getInterests().stream().map(Interest::getId).sorted().toList();
    }

    /** One name per stop, in travel order, a place named once even when the route returns to it. */
    protected List<String> stopNames(List<CorridorStop> stops) {
        Set<String> names = new LinkedHashSet<>();
        stops.forEach(stop -> names.add(stop.getDestination().getName()));
        return new ArrayList<>(names);
    }

    protected List<UUID> stopDestinationIds(List<CorridorStop> stops) {
        Set<UUID> ids = new LinkedHashSet<>();
        stops.forEach(stop -> ids.add(stop.getDestination().getId()));
        return new ArrayList<>(ids);
    }

    protected List<UUID> stopProvinceIds(List<CorridorStop> stops) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (CorridorStop stop : stops) {
            Destination place = stop.getDestination();
            if (place.getProvince() != null) {
                ids.add(place.getProvince().getId());
            }
        }
        return new ArrayList<>(ids);
    }

    protected List<UUID> stopRegionIds(List<CorridorStop> stops) {
        Set<UUID> ids = new LinkedHashSet<>();
        for (CorridorStop stop : stops) {
            Destination place = stop.getDestination();
            if (place.getProvince() != null && place.getProvince().getRegion() != null) {
                ids.add(place.getProvince().getRegion().getId());
            }
            for (Region theme : place.getTourismRegions()) {
                ids.add(theme.getId());
            }
        }
        return new ArrayList<>(ids);
    }
}
