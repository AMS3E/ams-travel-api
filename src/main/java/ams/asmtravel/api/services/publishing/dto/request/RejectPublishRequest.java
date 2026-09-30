package ams.asmtravel.api.services.publishing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A reason is required; sendBack returns it to the author as DRAFT. */
public record RejectPublishRequest(
        @NotBlank(message = "Say why") @Size(max = 2000, message = "A note may be at most 2000 characters") String note,
        Boolean sendBack
) {
}
