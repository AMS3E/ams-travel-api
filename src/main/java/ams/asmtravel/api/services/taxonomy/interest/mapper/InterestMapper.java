package ams.asmtravel.api.services.taxonomy.interest.mapper;

import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import ams.asmtravel.api.services.taxonomy.interest.dto.response.InterestResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InterestMapper {

    InterestResponse toInterestResponse(Interest interest);
}
