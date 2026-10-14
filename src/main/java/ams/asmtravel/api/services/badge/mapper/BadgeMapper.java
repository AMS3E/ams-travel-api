package ams.asmtravel.api.services.badge.mapper;

import ams.asmtravel.api.services.badge.domain.Badge;
import ams.asmtravel.api.services.badge.domain.UserBadge;
import ams.asmtravel.api.services.badge.dto.request.CreateBadgeRequest;
import ams.asmtravel.api.services.badge.dto.request.UpdateBadgeRequest;
import ams.asmtravel.api.services.badge.dto.response.BadgeResponse;
import ams.asmtravel.api.services.badge.dto.response.UserBadgeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BadgeMapper {

    BadgeResponse toResponse(Badge badge);

    /** Status and destinations are the service's to set. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "destinationIds", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Badge toBadge(CreateBadgeRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "destinationIds", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void apply(UpdateBadgeRequest request, @MappingTarget Badge badge);

    @Mapping(target = "id", source = "userBadge.id")
    @Mapping(target = "badgeId", source = "userBadge.badge.id")
    @Mapping(target = "code", source = "userBadge.badge.code")
    @Mapping(target = "name", source = "userBadge.badge.name")
    @Mapping(target = "iconKey", source = "userBadge.badge.iconKey")
    @Mapping(target = "type", source = "userBadge.badge.type")
    @Mapping(target = "level", source = "userBadge.badge.level")
    @Mapping(target = "threshold", source = "userBadge.badge.threshold")
    @Mapping(target = "progress", source = "userBadge.progress")
    @Mapping(target = "seasonId", source = "userBadge.seasonId")
    @Mapping(target = "earnedAt", source = "userBadge.earnedAt")
    @Mapping(target = "shareUrl", source = "shareUrl")
    UserBadgeResponse toUserBadgeResponse(UserBadge userBadge, String seasonName, String holderName, String shareUrl);
}
