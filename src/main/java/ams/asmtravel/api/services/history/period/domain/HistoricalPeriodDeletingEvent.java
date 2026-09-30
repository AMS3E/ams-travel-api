package ams.asmtravel.api.services.history.period.domain;

import java.util.UUID;

/** A period is about to be deleted: styles and timeline rows in it keep going, in no period. */
public record HistoricalPeriodDeletingEvent(UUID periodId) {
}
