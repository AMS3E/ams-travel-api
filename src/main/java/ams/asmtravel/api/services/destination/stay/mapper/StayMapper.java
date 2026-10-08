package ams.asmtravel.api.services.destination.stay.mapper;

import ams.asmtravel.api.services.destination.stay.domain.Stay;
import ams.asmtravel.api.services.destination.stay.dto.response.StayResponse;
import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class StayMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "destinationId", source = "destination.id")
    @Mapping(target = "provinceId", source = "province.id")
    @Mapping(target = "coverUrl", expression = "java(mediaService.url(stay.getCoverKey()))")
    @Mapping(target = "interestIds", expression = "java(interestIds(stay))")
    public abstract StayResponse toStayResponse(Stay stay);

    /** Stable order so responses do not reshuffle. */
    protected List<UUID> interestIds(Stay stay) {
        return stay.getInterests().stream().map(Interest::getId).sorted().toList();
    }
}
