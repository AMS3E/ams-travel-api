package ams.asmtravel.api.services.destination.stayroom.mapper;

import ams.asmtravel.api.services.destination.stayroom.domain.StayRoom;
import ams.asmtravel.api.services.destination.stayroom.dto.response.StayRoomResponse;
import ams.asmtravel.api.services.media.service.MediaService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class StayRoomMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "stayId", source = "stay.id")
    @Mapping(target = "currency", source = "stay.currency")
    @Mapping(target = "view", source = "roomView")
    @Mapping(target = "sleeps", expression = "java(room.sleeps())")
    @Mapping(target = "photoUrls", expression = "java(photoUrls(room))")
    public abstract StayRoomResponse toResponse(StayRoom room);

    protected List<String> photoUrls(StayRoom room) {
        return room.getPhotoKeys().stream().map(mediaService::url).toList();
    }
}
