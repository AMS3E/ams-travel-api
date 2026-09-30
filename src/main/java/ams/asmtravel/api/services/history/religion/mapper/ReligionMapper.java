package ams.asmtravel.api.services.history.religion.mapper;

import ams.asmtravel.api.services.history.religion.domain.Religion;
import ams.asmtravel.api.services.history.religion.dto.request.ReligionRequest;
import ams.asmtravel.api.services.history.religion.dto.response.ReligionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ReligionMapper {

    ReligionResponse toResponse(Religion religion);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "iconKey", expression = "java(request.iconKey() == null || request.iconKey().isBlank() ? null : request.iconKey().trim())")
    @Mapping(target = "createdAt", ignore = true)
    void apply(ReligionRequest request, @MappingTarget Religion religion);
}
