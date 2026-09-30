package ams.asmtravel.api.services.history.destinationmonarch.mapper;

import ams.asmtravel.api.services.history.destinationmonarch.domain.DestinationMonarch;
import ams.asmtravel.api.services.history.destinationmonarch.dto.response.DestinationMonarchResponse;
import ams.asmtravel.api.services.history.monarch.dto.response.MonarchResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DestinationMonarchMapper {

    /** @param monarch the monarch, as the monarch service has it */
    @Mapping(target = "destinationId", source = "link.id.destinationId")
    @Mapping(target = "monarchId", source = "link.id.monarchId")
    @Mapping(target = "name", source = "monarch.name")
    @Mapping(target = "title", source = "monarch.title")
    @Mapping(target = "reignStartYear", source = "monarch.reignStartYear")
    @Mapping(target = "reignEndYear", source = "monarch.reignEndYear")
    @Mapping(target = "description", source = "link.description")
    DestinationMonarchResponse toResponse(DestinationMonarch link, MonarchResponse monarch);
}
