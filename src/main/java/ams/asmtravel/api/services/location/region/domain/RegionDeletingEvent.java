package ams.asmtravel.api.services.location.region.domain;

import java.util.UUID;

/** Sent before a region is deleted, so no preferred region points at it afterwards. */
public record RegionDeletingEvent(UUID regionId) {
}
