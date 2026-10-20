package ams.asmtravel.api.services.activity.stamp.service;

import ams.asmtravel.api.services.activity.stamp.domain.StampTarget;
import ams.asmtravel.api.services.activity.stamp.dto.request.StampCodeRequest;
import ams.asmtravel.api.services.activity.stamp.dto.request.StampRequest;
import ams.asmtravel.api.services.activity.stamp.dto.response.StampCodeResponse;
import ams.asmtravel.api.services.activity.stamp.dto.response.StampResponse;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Digital stamps: check-ins on site, one per person, place and season. A check-in needs the code posted at
 * the place and the device standing within the radius; attractions, stays and food places are stamped alike.
 */
public interface StampService {

    /** Checks in with a scanned code: the first check-in of the season inside the radius collects the stamp. */
    StampResponse collect(UUID userId, StampRequest request);

    /** What a check-in answers when another one got the season's stamp a moment earlier. */
    StampResponse alreadyCollected(StampRequest request);

    /** A person's stamps, newest first. */
    List<StampResponse> findMine(UUID userId);

    // Check-in codes: what the CMS prints and posts on site.

    /** A place's codes, the current one first; empty when none was issued. */
    List<StampCodeResponse> findCodes(StampTarget targetType, UUID targetId);

    /** Issues a place's code, replacing the current one; the place must exist. */
    StampCodeResponse issueCode(StampCodeRequest request, UUID issuedBy);

    /** Withdraws a code for good, printed by mistake or never posted. */
    void withdrawCode(UUID codeId);

    /** The name of the place a current code belongs to, for the scan page; empty when the code is not current. */
    Optional<String> placeNameOf(String code);

    // For other services.

    /** Stamps collected in a window: what the Adventure badge counts. */
    long countBetween(UUID userId, Instant from, Instant until);

    /** How many of these places were stamped in a window: what a collection badge counts. */
    long countPlacesStamped(UUID userId, Collection<UUID> destinationIds, Instant from, Instant until);
}
