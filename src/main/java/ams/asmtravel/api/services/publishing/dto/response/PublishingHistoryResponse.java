package ams.asmtravel.api.services.publishing.dto.response;

import ams.asmtravel.api.services.publishing.domain.PublishingStatus;

import java.time.Instant;
import java.util.UUID;

/** One trip of a listing through the workflow, as its history lists it. */
public record PublishingHistoryResponse(
        UUID id,
        PublishingStatus status,
        String requestedByName,
        Instant requestedAt,
        String reviewerName,
        String reviewNote,
        Instant reviewedAt,
        Instant publishedAt,
        Instant unpublishedAt
) {
}
