package ams.asmtravel.api.services.activity.stamp.mapper;

import ams.asmtravel.api.services.activity.stamp.domain.DigitalStamp;
import ams.asmtravel.api.services.activity.stamp.domain.StampOutcome;
import ams.asmtravel.api.services.activity.stamp.dto.response.StampResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {StampOutcome.class, java.util.List.class})
public interface StampMapper {

    /** A stamp as a list shows it: collected, with no check-in to report on. */
    @Mapping(target = "outcome", expression = "java(StampOutcome.COLLECTED)")
    @Mapping(target = "distanceMeters", ignore = true)
    @Mapping(target = "radiusMeters", ignore = true)
    @Mapping(target = "badgesEarned", expression = "java(List.of())")
    StampResponse toResponse(DigitalStamp stamp);
}
