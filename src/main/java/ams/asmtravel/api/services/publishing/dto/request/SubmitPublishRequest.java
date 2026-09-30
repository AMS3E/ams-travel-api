package ams.asmtravel.api.services.publishing.dto.request;

import ams.asmtravel.api.services.publishing.domain.PublishingTargetType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Puts the caller's own listing forward; a request sent back is submitted again, not duplicated. */
public record SubmitPublishRequest(
        @NotNull PublishingTargetType targetType,
        @NotNull UUID targetId
) {
}
