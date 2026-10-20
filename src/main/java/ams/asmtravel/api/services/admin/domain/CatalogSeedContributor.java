package ams.asmtravel.api.services.admin.domain;

import org.springframework.core.Ordered;

/**
 * One service's share of the catalogue seed.
 * Runs in {@link #getOrder()} order, since a row cannot point at one not yet written.
 */
public interface CatalogSeedContributor extends Ordered {

    /** Rows that reference nothing: the vocabularies. */
    int TERMS = 100;

    /** Rows that reference the vocabularies: a region and its shelves. */
    int REGIONS = 200;

    /** Rows that reference a region, such as a province or a corridor. */
    int GROUPINGS = 300;

    /** Rows that reference a grouping, such as a destination. */
    int PLACES = 400;

    /** Rows that reference a place: stays and food places. */
    int LISTINGS = 450;

    /** Rows that reference a place: a gallery, a corridor's stops. */
    int DETAILS = 500;

    /** What travellers did: reviews, ratings, browses, and the desks' decisions. */
    int ENGAGEMENT = 600;

    /** Whether this service's rows already exist; one yes stops the whole seed, so nothing is written twice. */
    boolean alreadySeeded();

    /** Writes this service's rows, publishing each under the slug the seed uses. */
    void seed(CatalogSeedContext context);

    /**
     * Removes what this service seeded, so a reset can seed again. Run in reverse order, so a row
     * goes before the rows it points at; flush, so the rows are gone before the new ones arrive.
     */
    default void wipe() {
    }
}
