package ams.asmtravel.api.services.destination.stayroom.domain;

/** When a guest pays for the room: on arrival, a deposit first, or all of it in advance. */
public enum RoomPaymentTerms {
    PAY_AT_PROPERTY,
    DEPOSIT,
    PREPAYMENT
}
