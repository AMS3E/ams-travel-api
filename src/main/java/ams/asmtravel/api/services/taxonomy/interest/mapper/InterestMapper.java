package ams.asmtravel.api.services.taxonomy.interest.mapper;

import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import ams.asmtravel.api.services.taxonomy.interest.dto.response.InterestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class InterestMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "coverUrl", expression = "java(mediaService.url(interest.getCoverKey()))")
    public abstract InterestResponse toInterestResponse(Interest interest);
}
