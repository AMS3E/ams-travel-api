package ams.asmtravel.api.services.season.mapper;

import ams.asmtravel.api.services.season.domain.Season;
import ams.asmtravel.api.services.season.domain.UserSeasonProgress;
import ams.asmtravel.api.services.season.dto.request.CreateSeasonRequest;
import ams.asmtravel.api.services.season.dto.request.UpdateSeasonRequest;
import ams.asmtravel.api.services.season.dto.response.SeasonResponse;
import ams.asmtravel.api.services.season.dto.response.UserSeasonProgressResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SeasonMapper {

    SeasonResponse toResponse(Season season);

    /** Everything but the flags the service decides. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Season toSeason(CreateSeasonRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void apply(UpdateSeasonRequest request, @MappingTarget Season season);

    /** A person's row for a season, or zeros when they have none yet. */
    default UserSeasonProgressResponse toProgressResponse(Season season, UserSeasonProgress progress) {
        if (progress == null) {
            return new UserSeasonProgressResponse(season.getId(), season.getName(), season.getType(), season.getStatus(),
                    0, 0, 0, 0, 0, null, null);
        }
        return new UserSeasonProgressResponse(season.getId(), season.getName(), season.getType(), season.getStatus(),
                progress.getPoints(), progress.getRatingCount(), progress.getReviewCount(),
                progress.getUniqueRatedTargetCount(), progress.getUniqueReviewedTargetCount(),
                progress.getStartedAt(), progress.getUpdatedAt());
    }
}
