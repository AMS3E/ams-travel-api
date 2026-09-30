package ams.asmtravel.api.services.location.province.mapper;

import ams.asmtravel.api.services.location.province.domain.Province;
import ams.asmtravel.api.services.location.province.dto.response.ProvinceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProvinceMapper {

    @Mapping(target = "regionId", source = "region.id")
    ProvinceResponse toProvinceResponse(Province province);
}
