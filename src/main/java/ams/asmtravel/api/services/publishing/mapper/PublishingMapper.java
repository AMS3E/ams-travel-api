package ams.asmtravel.api.services.publishing.mapper;

import ams.asmtravel.api.services.publishing.domain.PublishingRequest;
import ams.asmtravel.api.services.publishing.dto.response.PublishingHistoryResponse;
import ams.asmtravel.api.services.publishing.dto.response.PublishingResponse;
import ams.asmtravel.api.services.publishing.service.PublishingService;
import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.user.user.domain.UserSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PublishingMapper {

    @Mapping(target = "id", source = "request.id")
    @Mapping(target = "requestedByName", source = "requester.fullName")
    @Mapping(target = "reviewerName", source = "reviewer.fullName")
    @Mapping(target = "requestedByRole", source = "role")
    @Mapping(target = "desk", source = "desk")
    PublishingResponse toResponse(PublishingRequest request, UserSummary requester, UserSummary reviewer,
                                  RoleCode role, PublishingService.Desk desk);

    @Mapping(target = "id", source = "request.id")
    @Mapping(target = "requestedByName", source = "requester.fullName")
    @Mapping(target = "reviewerName", source = "reviewer.fullName")
    PublishingHistoryResponse toHistoryResponse(PublishingRequest request, UserSummary requester, UserSummary reviewer);
}
