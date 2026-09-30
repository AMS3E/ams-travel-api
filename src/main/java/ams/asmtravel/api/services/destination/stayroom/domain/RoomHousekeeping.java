package ams.asmtravel.api.services.destination.stayroom.domain;

/** How often the room is cleaned during a stay. */
public enum RoomHousekeeping {

    DAILY,

    /** Cleaned when the guest asks. */
    ON_REQUEST,

    WEEKLY,

    /** Not cleaned during the stay; fresh at check-in. */
    NONE
}
