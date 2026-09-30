package ams.asmtravel.api.services.verification.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Verify (needs evidence file or reference number) or revoke (needs a note).
 *
 * @param evidenceKey key from {@code POST /medias?category=verification}; image or PDF only
 */
public record VerificationRequest(
        @NotNull UUID destinationId,
        @NotNull(message = "Say whether the destination is verified") Boolean verified,
        @Size(max = 120, message = "A reference may be at most 120 characters") String reference,
        @Size(max = 2000, message = "A note may be at most 2000 characters") String note,
        @Size(max = 512) String evidenceKey
) {
}
