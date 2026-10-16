package ams.asmtravel.api.services.activity.popular.domain;

import java.util.UUID;

/** One place's browses in a window: how many different signed-in visitors opened it. */
public record BrowseTotal(UUID targetId, Long browses) {
}
