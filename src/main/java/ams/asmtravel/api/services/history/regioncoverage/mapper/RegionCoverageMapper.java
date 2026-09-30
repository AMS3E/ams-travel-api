package ams.asmtravel.api.services.history.regioncoverage.mapper;

import ams.asmtravel.api.services.history.regioncoverage.domain.RegionCoverage;
import ams.asmtravel.api.services.history.regioncoverage.dto.response.RegionCoverageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RegionCoverageMapper {

    @Mapping(target = "regionId", source = "coverage.region.id")
    @Mapping(target = "destinationId", source = "coverage.destination.id")
    @Mapping(target = "destinationSlug", source = "coverage.destination.slug")
    @Mapping(target = "destinationName", source = "coverage.destination.name")
    @Mapping(target = "historicalPeriodName", source = "periodName")
    RegionCoverageResponse toRegionCoverageResponse(RegionCoverage coverage, String periodName);
}
