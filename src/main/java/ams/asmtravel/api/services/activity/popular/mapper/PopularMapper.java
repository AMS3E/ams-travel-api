package ams.asmtravel.api.services.activity.popular.mapper;

import ams.asmtravel.api.services.activity.popular.domain.BrowseEvent;
import ams.asmtravel.api.services.activity.popular.domain.PopularPlace;
import ams.asmtravel.api.services.activity.popular.dto.request.BrowseEventRequest;
import ams.asmtravel.api.services.activity.popular.dto.response.PopularResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface PopularMapper {

    PopularResponse.Place toPlace(PopularPlace place);

    default PopularResponse toResponse(int days, UUID regionId, List<PopularPlace> places) {
        return new PopularResponse(days, regionId, places.stream().map(this::toPlace).toList());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "createdAt", ignore = true)
    BrowseEvent toEvent(BrowseEventRequest request, UUID userId);
}
