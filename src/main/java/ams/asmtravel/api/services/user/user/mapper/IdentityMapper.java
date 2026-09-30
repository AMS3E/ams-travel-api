package ams.asmtravel.api.services.user.user.mapper;

import ams.asmtravel.api.services.user.user.domain.AuthIdentity;
import ams.asmtravel.api.services.user.user.dto.response.IdentityResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IdentityMapper {

    @Mapping(target = "userId", source = "user.id")
    IdentityResponse toResponse(AuthIdentity identity);
}
