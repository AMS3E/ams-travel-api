package ams.asmtravel.api.services.destination.stayroom.domain;

/** Kind of room. A dorm is sold per bed ({@link #DORM_BED}) or as a whole room. */
public enum RoomType {
    SINGLE,
    DOUBLE,
    TWIN,
    TWIN_DOUBLE,
    TRIPLE,
    QUADRUPLE,
    FAMILY,
    SUITE,
    STUDIO,
    APARTMENT,
    BUNGALOW,
    VILLA,
    DORM_BED,
    DORM_ROOM
}
