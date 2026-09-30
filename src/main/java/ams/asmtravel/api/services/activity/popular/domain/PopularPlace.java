package ams.asmtravel.api.services.activity.popular.domain;

import java.util.UUID;

/** One place on Popular, in list order. */
public record PopularPlace(UUID destinationId, long browses, PopularReason reason) {
}
