package ams.asmtravel.api.services.history.monarch.mapper;

import ams.asmtravel.api.services.history.monarch.domain.Monarch;
import ams.asmtravel.api.services.history.monarch.dto.request.MonarchRequest;
import ams.asmtravel.api.services.history.monarch.dto.response.MonarchResponse;
import ams.asmtravel.api.services.media.service.MediaService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class MonarchMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "imageUrl", expression = "java(mediaService.url(monarch.getImageKey()))")
    public abstract MonarchResponse toResponse(Monarch monarch);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "imageKey", expression = "java(request.imageKey() == null || request.imageKey().isBlank() ? null : request.imageKey())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract void apply(MonarchRequest request, @MappingTarget Monarch monarch);
}
