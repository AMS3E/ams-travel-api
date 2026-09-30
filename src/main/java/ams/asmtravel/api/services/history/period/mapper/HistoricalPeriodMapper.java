package ams.asmtravel.api.services.history.period.mapper;

import ams.asmtravel.api.services.history.period.domain.HistoricalPeriod;
import ams.asmtravel.api.services.history.period.dto.request.HistoricalPeriodRequest;
import ams.asmtravel.api.services.history.period.dto.response.HistoricalPeriodResponse;
import ams.asmtravel.api.services.media.service.MediaService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class HistoricalPeriodMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "coverUrl", expression = "java(mediaService.url(period.getCoverKey()))")
    public abstract HistoricalPeriodResponse toResponse(HistoricalPeriod period);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", expression = "java(request.name().trim())")
    @Mapping(target = "coverKey", expression = "java(request.coverKey() == null || request.coverKey().isBlank() ? null : request.coverKey())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    public abstract void apply(HistoricalPeriodRequest request, @MappingTarget HistoricalPeriod period);
}
