package ams.asmtravel.api.services.destination.destination.mapper;

import ams.asmtravel.api.services.destination.destination.domain.Destination;
import ams.asmtravel.api.services.destination.destination.domain.DestinationSummary;
import ams.asmtravel.api.services.destination.destination.dto.response.DestinationResponse;
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
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        imports = {Category.class, Tag.class, Interest.class, Region.class})
public abstract class DestinationMapper {

    @Autowired
    protected MediaService mediaService;

    @Mapping(target = "provinceId", source = "province.id")
    @Mapping(target = "regionId", source = "province.region.id")
    @Mapping(target = "coverUrl", expression = "java(mediaService.url(destination.getCoverKey()))")
    @Mapping(target = "categoryIds", expression = "java(ids(destination.getCategories(), Category::getId))")
    @Mapping(target = "tagIds", expression = "java(ids(destination.getTags(), Tag::getId))")
    @Mapping(target = "interestIds", expression = "java(ids(destination.getInterests(), Interest::getId))")
    @Mapping(target = "tourismRegionIds", expression = "java(ids(destination.getTourismRegions(), Region::getId))")
    public abstract DestinationResponse toDestinationResponse(Destination destination);

    @Mapping(target = "featured", source = "isFeatured")
    @Mapping(target = "provinceId", source = "province.id")
    @Mapping(target = "regionId", source = "province.region.id")
    @Mapping(target = "categoryIds", expression = "java(idSet(destination.getCategories(), Category::getId))")
    @Mapping(target = "interestIds", expression = "java(idSet(destination.getInterests(), Interest::getId))")
    @Mapping(target = "tourismRegionIds", expression = "java(idSet(destination.getTourismRegions(), Region::getId))")
    public abstract DestinationSummary toSummary(Destination destination);

    /** Stable order so responses do not reshuffle. */
    @Named("ids")
    protected <T> List<UUID> ids(Collection<T> linked, Function<T, UUID> id) {
        return linked.stream().map(id).sorted().toList();
    }

    @Named("idSet")
    protected <T> Set<UUID> idSet(Collection<T> linked, Function<T, UUID> id) {
        return linked.stream().map(id).collect(Collectors.toUnmodifiableSet());
    }
}
