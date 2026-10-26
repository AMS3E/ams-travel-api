package ams.asmtravel.api.services.verification.dto.request;

import ams.asmtravel.api.services.verification.domain.VerificationBasis;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Verify, on what the verification rests on, or revoke (needs a note). A paid
 * verification needs the paperwork, the file or its number; the travellers'
 * score cannot be filed.
 *
 * @param basis       what it rests on; required to verify, ignored to revoke
 * @param evidenceKey key from {@code POST /medias?category=verification}; image or PDF only
 */
public record VerificationRequest(
        @NotNull UUID destinationId,
        @NotNull(message = "Say whether the destination is verified") Boolean verified,
        VerificationBasis basis,
        @Size(max = 120, message = "A reference may be at most 120 characters") String reference,
        @Size(max = 2000, message = "A note may be at most 2000 characters") String note,
        @Size(max = 512) String evidenceKey
) {
}
