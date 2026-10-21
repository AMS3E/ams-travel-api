package ams.asmtravel.api.services.destination.food.mapper;

import ams.asmtravel.api.services.destination.food.domain.Food;
import ams.asmtravel.api.services.destination.food.domain.OpeningHours;
import ams.asmtravel.api.services.destination.food.dto.response.FoodResponse;
import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import ams.asmtravel.api.services.taxonomy.tag.domain.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Mapper(componentModel = "spring", imports = {Category.class, Tag.class, Interest.class, OpeningHours.class})
public abstract class FoodMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "destinationId", source = "destination.id")
    @Mapping(target = "provinceId", source = "province.id")
    @Mapping(target = "coverUrl", expression = "java(mediaService.url(food.getCoverKey()))")
    @Mapping(target = "openNow", expression = "java(OpeningHours.openNow(food.getOpeningHours()))")
    @Mapping(target = "todayHours", expression = "java(OpeningHours.today(food.getOpeningHours()))")
    @Mapping(target = "categoryIds", expression = "java(ids(food.getCategories(), Category::getId))")
    @Mapping(target = "tagIds", expression = "java(ids(food.getTags(), Tag::getId))")
    @Mapping(target = "interestIds", expression = "java(ids(food.getInterests(), Interest::getId))")
    @Mapping(target = "missingKhmer", expression = "java(food.missingKhmer())")
    public abstract FoodResponse toFoodResponse(Food food);

    /** Stable order so responses do not reshuffle. */
    @Named("ids")
    protected <T> List<UUID> ids(Collection<T> linked, Function<T, UUID> id) {
        return linked.stream().map(id).sorted().toList();
    }
}
