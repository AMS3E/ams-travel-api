package ams.asmtravel.api.services.activity.popular.domain;

import java.util.UUID;

/** One row of the Popular list, and why it is there. */
public record PopularItem(BrowseTarget type, UUID id, long browses, PopularReason reason) {
}
