package ams.asmtravel.api.services.taxonomy.interestgroup.mapper;

import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.taxonomy.interestgroup.domain.InterestGroup;
import ams.asmtravel.api.services.taxonomy.interestgroup.dto.response.InterestGroupResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class InterestGroupMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "coverUrl", expression = "java(mediaService.url(group.getCoverKey()))")
    public abstract InterestGroupResponse toInterestGroupResponse(InterestGroup group);
}
