package ams.asmtravel.api.services.location.province.domain;

import java.util.UUID;

/** Sent before a province is deleted; the destination service throws 409 if it still has destinations. */
public record ProvinceDeletingEvent(UUID provinceId) {
}
