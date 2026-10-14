package ams.asmtravel.api.services.destination.destination.domain;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/**
 * What the app's search asks of published destinations; every part is optional.
 *
 * @param regionIds   any of these regions: a zone through the province, a tourism region through the shelf
 * @param provinceIds any of these provinces
 * @param seal        when set, only places whose star ratings earn the Traveler seal
 */
public record DestinationSearch(String q, Set<UUID> regionIds, Set<UUID> provinceIds, Set<UUID> categoryIds, Set<UUID> tagIds,
                                Set<UUID> interestIds, Seal seal) {

    /** The Traveler seal's bar: this many active star ratings at this average or better. */
    public record Seal(long minRatings, BigDecimal minAverage) {
    }
}
