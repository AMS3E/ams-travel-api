package ams.asmtravel.api.services.location.province.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** A province as other services need it: name, zone and centre, with none of the counting a full response does. */
public record ProvinceSummary(UUID id, String name, String slug, UUID regionId, BigDecimal latitude, BigDecimal longitude) {
}
