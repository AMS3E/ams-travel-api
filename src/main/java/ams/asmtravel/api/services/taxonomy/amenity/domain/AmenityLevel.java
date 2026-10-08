package ams.asmtravel.api.services.taxonomy.amenity.domain;

/** Where an amenity is offered: by the property to every guest, in one room, or both. */
public enum AmenityLevel {
    PROPERTY,
    ROOM,
    BOTH;

    /** Whether an amenity of this level is offered at the level asked for. */
    public boolean offeredAt(AmenityLevel wanted) {
        return this == BOTH || this == wanted;
    }
}
