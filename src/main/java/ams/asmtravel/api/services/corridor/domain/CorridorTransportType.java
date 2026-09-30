package ams.asmtravel.api.services.corridor.domain;

/**
 * How a traveller reaches a stop from the previous one.
 * A boat follows nearby roads but at boat pace plus boarding; a flight is a straight line.
 */
public enum CorridorTransportType {
    CAR(RoadTravelMode.DRIVE, 0, 0, false),
    MINIVAN(RoadTravelMode.DRIVE, 0, 0, false),
    BUS(RoadTravelMode.DRIVE, 0, 0, false),
    TUK_TUK(RoadTravelMode.DRIVE, 0, 0, false),
    MOTORBIKE(RoadTravelMode.TWO_WHEELER, 0, 0, false),
    BICYCLE(RoadTravelMode.BICYCLE, 15, 0, false),
    WALK(RoadTravelMode.WALK, 0, 0, false),
    BOAT(RoadTravelMode.DRIVE, 25, 15, true),
    FLIGHT(null, 600, 90, true);

    private final RoadTravelMode roadMode;
    private final int averageKmh;
    private final int boardingMinutes;
    private final boolean ownPace;

    CorridorTransportType(RoadTravelMode roadMode, int averageKmh, int boardingMinutes, boolean ownPace) {
        this.roadMode = roadMode;
        this.averageKmh = averageKmh;
        this.boardingMinutes = boardingMinutes;
        this.ownPace = ownPace;
    }

    /** CAR when no transport is chosen. */
    public static CorridorTransportType orDefault(CorridorTransportType type) {
        return type == null ? CAR : type;
    }

    /** Road mode used for routing; null for a flight. */
    public RoadTravelMode roadMode() {
        return roadMode;
    }

    /** Speed for legs timed by speed; 0 means use the routed time. */
    public int averageKmh() {
        return averageKmh;
    }

    /** Boarding time added to a crossing or flight. */
    public int boardingMinutes() {
        return boardingMinutes;
    }

    /** Timed at its own speed even on a road (a boat is slower than the road). */
    public boolean ownPace() {
        return ownPace;
    }
}
