package ams.asmtravel.api.services.taxonomy.category.mapper;

import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.taxonomy.category.dto.response.CategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class CategoryMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "coverUrl", expression = "java(mediaService.url(category.getCoverKey()))")
    public abstract CategoryResponse toCategoryResponse(Category category);
}
