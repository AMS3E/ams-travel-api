package ams.asmtravel.api.services.location.region.mapper;

import ams.asmtravel.api.services.taxonomy.category.domain.Category;
import ams.asmtravel.api.services.location.region.domain.Region;
import ams.asmtravel.api.services.taxonomy.interest.domain.Interest;
import ams.asmtravel.api.services.taxonomy.tag.domain.Tag;
import ams.asmtravel.api.services.location.region.dto.response.GeoBounds;
import ams.asmtravel.api.services.location.region.dto.response.RegionResponse;
import ams.asmtravel.api.services.media.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

@Mapper(componentModel = "spring", imports = {Category.class, Tag.class, Interest.class})
public abstract class RegionMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "id", source = "region.id")
    @Mapping(target = "coverUrl", expression = "java(mediaService.url(region.getCoverKey()))")
    @Mapping(target = "mapUrl", expression = "java(mediaService.url(region.getMapKey()))")
    @Mapping(target = "categoryIds", expression = "java(inOrder(region.getCategories(), Category::getId))")
    @Mapping(target = "tagIds", expression = "java(inOrder(region.getTags(), Tag::getId))")
    @Mapping(target = "interestIds", expression = "java(ids(region.getInterests(), Interest::getId))")
    @Mapping(target = "latitude", expression = "java(region.getLatitude() != null || bounds == null ? region.getLatitude() : bounds.centreLatitude())")
    @Mapping(target = "longitude", expression = "java(region.getLongitude() != null || bounds == null ? region.getLongitude() : bounds.centreLongitude())")
    @Mapping(target = "centreSet", expression = "java(region.getLatitude() != null && region.getLongitude() != null)")
    @Mapping(target = "bounds", source = "bounds")
    public abstract RegionResponse toRegionResponse(Region region, GeoBounds bounds);

    /** Linked ids in a stable order, so a response does not reshuffle between reads. */
    @Named("ids")
    protected <T> List<UUID> ids(Collection<T> linked, Function<T, UUID> id) {
        return linked.stream().map(id).sorted().toList();
    }

    /** Linked ids in the region's own order, as the curators numbered them. */
    @Named("inOrder")
    protected <T> List<UUID> inOrder(List<T> linked, Function<T, UUID> id) {
        return linked.stream().filter(Objects::nonNull).map(id).toList();
    }
}
