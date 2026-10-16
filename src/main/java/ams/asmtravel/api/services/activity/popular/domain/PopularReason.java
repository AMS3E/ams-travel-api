package ams.asmtravel.api.services.activity.popular.domain;

/**
 * Why a place is on the Popular list. Browses come first; when fewer places than asked have any, the
 * rest is filled in a stable order so the row is never empty: verified places, then the best rated,
 * then the catalogue A to Z. The app may show the filler plainly or mark it.
 */
public enum PopularReason {
    MOST_BROWSED,
    VERIFIED,
    TOP_RATED,
    CATALOGUE
}
