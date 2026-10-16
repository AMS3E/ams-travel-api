package ams.asmtravel.api.services.location.province.mapper;

import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.province.domain.ProvinceStanding;
import ams.asmtravel.api.services.location.province.domain.ProvinceSummary;
import ams.asmtravel.api.services.location.province.dto.response.ProvinceResponse;
import ams.asmtravel.api.services.media.service.MediaService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class ProvinceMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "id", source = "province.id")
    @Mapping(target = "regionId", source = "province.region.id")
    @Mapping(target = "coverUrl", expression = "java(mediaService.url(province.getCoverKey()))")
    @Mapping(target = "bounds", source = "standing.bounds")
    @Mapping(target = "placeCount", source = "standing.placeCount")
    @Mapping(target = "averageRating", source = "standing.averageRating")
    @Mapping(target = "reviewCount", source = "standing.reviewCount")
    public abstract ProvinceResponse toProvinceResponse(Province province, ProvinceStanding standing);

    @Mapping(target = "regionId", source = "region.id")
    public abstract ProvinceSummary toSummary(Province province);
}
