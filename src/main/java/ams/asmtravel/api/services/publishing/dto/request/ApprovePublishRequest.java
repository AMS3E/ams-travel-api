package ams.asmtravel.api.services.publishing.dto.request;

import jakarta.validation.constraints.Size;

/** @param note optional words for the author */
public record ApprovePublishRequest(
        @Size(max = 2000, message = "A note may be at most 2000 characters") String note
) {
}
