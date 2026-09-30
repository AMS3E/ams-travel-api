package ams.asmtravel.api.services.history.history.mapper;

import ams.asmtravel.api.services.history.history.domain.DestinationHistory;
import ams.asmtravel.api.services.history.history.dto.request.DestinationHistoryRequest;
import ams.asmtravel.api.services.history.history.dto.response.DestinationHistoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface HistoryMapper {

    /** @param periodName the period's name, or null when it has none */
    @Mapping(target = "id", source = "moment.id")
    @Mapping(target = "destinationId", source = "moment.destination.id")
    @Mapping(target = "historicalPeriodId", source = "moment.historicalPeriodId")
    @Mapping(target = "historicalPeriodName", source = "periodName")
    @Mapping(target = "title", source = "moment.title")
    @Mapping(target = "description", source = "moment.description")
    @Mapping(target = "eventYear", source = "moment.eventYear")
    @Mapping(target = "startDate", source = "moment.startDate")
    @Mapping(target = "endDate", source = "moment.endDate")
    @Mapping(target = "sourceReference", source = "moment.sourceReference")
    @Mapping(target = "createdAt", source = "moment.createdAt")
    @Mapping(target = "updatedAt", source = "moment.updatedAt")
    DestinationHistoryResponse toResponse(DestinationHistory moment, String periodName);

    /** Everything but the destination, which the service checks the caller may edit. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "destination", ignore = true)
    @Mapping(target = "title", expression = "java(request.title().trim())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void apply(DestinationHistoryRequest request, @MappingTarget DestinationHistory moment);
}
