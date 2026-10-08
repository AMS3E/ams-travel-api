package ams.asmtravel.api.services.taxonomy.amenity.domain;

import java.util.UUID;

/**
 * Published before an amenity is deleted, in the same transaction. A stay or
 * a room that still lists the slug refuses, so the amenity is retired instead.
 */
public record AmenityDeletingEvent(UUID amenityId, String slug) {
}
