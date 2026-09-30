package ams.asmtravel.api.services.point.dto.request;

import java.util.UUID;

/** {@code ?seasonId=}: one season's points; all seasons when left out. */
public record PointHistoryRequest(UUID seasonId) {
}
