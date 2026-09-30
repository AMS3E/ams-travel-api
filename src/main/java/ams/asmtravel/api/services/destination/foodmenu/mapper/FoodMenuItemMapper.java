package ams.asmtravel.api.services.destination.foodmenu.mapper;

import ams.asmtravel.api.services.destination.foodmenu.domain.FoodMenuItem;
import ams.asmtravel.api.services.destination.foodmenu.dto.response.FoodMenuItemResponse;
import ams.asmtravel.api.services.media.service.MediaService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class FoodMenuItemMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "foodId", source = "food.id")
    @Mapping(target = "currency", source = "food.currency")
    @Mapping(target = "photoUrls", expression = "java(photoUrls(item))")
    public abstract FoodMenuItemResponse toResponse(FoodMenuItem item);

    protected List<String> photoUrls(FoodMenuItem item) {
        return item.getPhotoKeys().stream().map(mediaService::url).toList();
    }
}
