package ams.asmtravel.api.services.verification.service;

import ams.asmtravel.api.services.verification.dto.request.VerificationRequest;
import ams.asmtravel.api.services.verification.dto.response.VerificationResponse;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.UUID;

/**
 * Destination verification: a desk's decision on what it rests on, or the
 * travellers' seal earned from reviews. Status is public; decisions and
 * evidence are for verifying desks.
 */
public interface VerificationService {

    /** Places the caller may verify, each with its latest decision (or none). */
    List<VerificationResponse> findQueue(Authentication authentication);

    /** One place's decisions, newest first; 403 unless the caller's desks reach it. */
    List<VerificationResponse> findHistory(UUID destinationId, Authentication authentication);

    /**
     * Verifies, renews or revokes. Verifying names its basis and brings what that
     * basis needs; revoking needs a note and a decision to revoke — the travellers'
     * seal is not one.
     */
    VerificationResponse record(VerificationRequest request, UUID deciderId, Authentication authentication);

    /** Public status; without ids, all verified published places. */
    List<VerificationResponse> findStatuses(List<UUID> destinationIds);
}
