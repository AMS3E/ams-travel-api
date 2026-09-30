package ams.asmtravel.api.services.taxonomy.category.mapper;

import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.taxonomy.category.dto.response.CategoryResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toCategoryResponse(Category category);
}
