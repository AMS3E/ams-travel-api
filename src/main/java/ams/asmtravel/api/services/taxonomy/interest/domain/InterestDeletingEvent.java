package ams.asmtravel.api.services.taxonomy.interest.domain;

import java.util.UUID;

/** Published before an interest is deleted, in the same transaction; destinations and food drop their links. */
public record InterestDeletingEvent(UUID interestId) {
}
