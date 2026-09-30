package ams.asmtravel.api.services.favorite.mapper;

import ams.asmtravel.api.services.favorite.domain.Favorite;
import ams.asmtravel.api.services.favorite.dto.response.FavoriteResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FavoriteMapper {

    @Mapping(target = "targetType", source = "id.targetType")
    @Mapping(target = "targetId", source = "id.targetId")
    FavoriteResponse toFavoriteResponse(Favorite favorite);
}
