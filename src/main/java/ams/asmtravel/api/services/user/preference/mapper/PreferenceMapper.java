package ams.asmtravel.api.services.user.preference.mapper;

import ams.asmtravel.api.services.user.preference.domain.UserPreferences;
import ams.asmtravel.api.services.user.preference.dto.request.PreferenceRequest;
import ams.asmtravel.api.services.user.preference.dto.response.PreferenceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PreferenceMapper {

    PreferenceResponse toResponse(UserPreferences preferences);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void update(PreferenceRequest request, @MappingTarget UserPreferences preferences);
}
