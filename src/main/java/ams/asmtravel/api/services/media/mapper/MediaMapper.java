package ams.asmtravel.api.services.media.mapper;

import ams.asmtravel.api.services.media.domain.Media;
import ams.asmtravel.api.services.media.dto.response.MediaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** The link is passed in, so the mapper needs no service. */
@Mapper(componentModel = "spring")
public interface MediaMapper {

    @Mapping(target = "mediaUrl", source = "mediaUrl")
    MediaResponse toResponse(Media media, String mediaUrl);
}
