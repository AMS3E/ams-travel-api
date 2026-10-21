package ams.asmtravel.api.services.destination.activity.mapper;

import ams.asmtravel.api.services.destination.activity.domain.Activity;
import ams.asmtravel.api.services.destination.activity.dto.response.ActivityResponse;
import ams.asmtravel.api.services.location.region.domain.Region;
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

@Mapper(componentModel = "spring", imports = {Category.class, Tag.class, Interest.class, Region.class})
public abstract class ActivityMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "destinationId", source = "destination.id")
    @Mapping(target = "provinceId", source = "province.id")
    @Mapping(target = "coverUrl", expression = "java(mediaService.url(activity.getCoverKey()))")
    @Mapping(target = "categoryIds", expression = "java(ids(activity.getCategories(), Category::getId))")
    @Mapping(target = "tagIds", expression = "java(ids(activity.getTags(), Tag::getId))")
    @Mapping(target = "interestIds", expression = "java(ids(activity.getInterests(), Interest::getId))")
    @Mapping(target = "tourismRegionIds", expression = "java(ids(activity.getTourismRegions(), Region::getId))")
    @Mapping(target = "missingKhmer", expression = "java(activity.missingKhmer())")
    public abstract ActivityResponse toActivityResponse(Activity activity);

    /** Stable order so responses do not reshuffle. */
    @Named("ids")
    protected <T> List<UUID> ids(Collection<T> linked, Function<T, UUID> id) {
        return linked.stream().map(id).sorted().toList();
    }
}
