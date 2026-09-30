package ams.asmtravel.api.services.taxonomy.tag.mapper;

import ams.asmtravel.api.services.taxonomy.tag.domain.Tag;
import ams.asmtravel.api.services.taxonomy.tag.dto.response.TagResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {

    TagResponse toTagResponse(Tag tag);
}
