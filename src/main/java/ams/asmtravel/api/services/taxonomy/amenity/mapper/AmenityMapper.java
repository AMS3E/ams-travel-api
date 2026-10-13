package ams.asmtravel.api.services.taxonomy.amenity.mapper;

import ams.asmtravel.api.services.taxonomy.amenity.domain.Amenity;
import ams.asmtravel.api.services.taxonomy.amenity.dto.response.AmenityResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AmenityMapper {

    AmenityResponse toAmenityResponse(Amenity amenity);
}
