package ams.asmtravel.api.services.publishing.dto.response;

import ams.asmtravel.api.services.publishing.domain.PublishingStatus;
import ams.asmtravel.api.services.publishing.domain.PublishingTargetType;
import ams.asmtravel.api.services.publishing.service.PublishingService;
import ams.asmtravel.api.services.authorization.role.domain.RoleCode;

import java.time.Instant;
import java.util.UUID;

/** A publishing request with names beside the ids. */
public record PublishingResponse(
        UUID id,
        PublishingTargetType targetType,
        UUID targetId,
        String targetName,
        UUID requestedBy,
        String requestedByName,
        RoleCode requestedByRole,
        PublishingService.Desk desk,
        PublishingStatus status,
        UUID reviewerId,
        String reviewerName,
        String reviewNote,
        Instant requestedAt,
        Instant reviewedAt,
        Instant publishedAt,
        Instant unpublishedAt
) {
}
