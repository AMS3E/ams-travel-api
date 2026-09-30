package ams.asmtravel.api.services.activity.stamp.service;

import ams.asmtravel.api.services.activity.stamp.dto.request.StampRequest;
import ams.asmtravel.api.services.activity.stamp.dto.response.StampResponse;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Digital stamps: check-ins on site, one per person, place and day in Cambodia. */
public interface StampService {

    /** Checks in at a published destination; the first check-in of the day inside the radius collects a stamp. */
    StampResponse collect(UUID userId, StampRequest request);

    /** What a check-in answers when another one got today's stamp a moment earlier. */
    StampResponse alreadyCollected(UUID destinationId);

    /** A person's stamps, newest first. */
    List<StampResponse> findMine(UUID userId);

    // For other services.

    /** Stamps collected in a window: what the Adventure badge counts. */
    long countBetween(UUID userId, Instant from, Instant until);

    /** How many of these places were stamped in a window: what a collection badge counts. */
    long countPlacesStamped(UUID userId, Collection<UUID> destinationIds, Instant from, Instant until);
}
