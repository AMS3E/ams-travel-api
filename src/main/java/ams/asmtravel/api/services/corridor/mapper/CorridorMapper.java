package ams.asmtravel.api.services.corridor.mapper;

import ams.asmtravel.api.services.corridor.domain.Corridor;
import ams.asmtravel.api.services.corridor.domain.CorridorRouteStatus;
import ams.asmtravel.api.services.corridor.dto.response.CorridorResponse;
import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class CorridorMapper {

    @Autowired
    protected MediaService mediaService;

    /** The overview, with stop count and route status. */
    @Mapping(target = "coverUrl", expression = "java(mediaService.url(corridor.getCoverKey()))")
    @Mapping(target = "isActive", expression = "java(corridor.isPublished())")
    @Mapping(target = "interestIds", expression = "java(interestIds(corridor))")
    public abstract CorridorResponse toResponse(Corridor corridor, int stopCount, CorridorRouteStatus routeStatus);

    /** Sorted, so the order is stable between reads. */
    protected List<UUID> interestIds(Corridor corridor) {
        return corridor.getInterests().stream().map(Interest::getId).sorted().toList();
    }
}
