package ams.asmtravel.api.services.history.architecture.mapper;

import ams.asmtravel.api.services.history.architecture.domain.ArchitectureStyle;
import ams.asmtravel.api.services.history.architecture.dto.request.ArchitectureStyleRequest;
import ams.asmtravel.api.services.history.architecture.dto.response.ArchitectureStyleResponse;
import ams.asmtravel.api.services.media.service.MediaService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ArchitectureStyleMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "imageUrl", expression = "java(mediaService.url(style.getImageKey()))")
    public abstract ArchitectureStyleResponse toResponse(ArchitectureStyle style);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "imageKey", expression = "java(request.imageKey() == null || request.imageKey().isBlank() ? null : request.imageKey())")
    @Mapping(target = "createdAt", ignore = true)
    public abstract void apply(ArchitectureStyleRequest request, @MappingTarget ArchitectureStyle style);
}
