package ams.asmtravel.api.services.destination.stayroom.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * What one room costs for given nights, night by night: a weekend night at the room's weekend price when it
 * has one, every other night at its nightly price, the sum taken once per room asked for. The app shows the
 * total for the nights, never a nightly price.
 *
 * @param total         null when the room has no price
 * @param fitsGuests    the adults and children fit in that many rooms of this kind
 * @param enoughRooms   the stay has that many rooms of this kind
 * @param meetsMinNights the nights are not fewer than the room's shortest stay
 */
public record RoomQuote(
        UUID roomId,
        String name,
        BigDecimal pricePerNight,
        BigDecimal weekendPrice,
        List<Night> nights,
        int weekendNightCount,
        BigDecimal total,
        boolean fitsGuests,
        boolean enoughRooms,
        boolean meetsMinNights,
        int minNights,
        boolean breakfastIncluded,
        boolean taxesIncluded
) {
    /** One night of the stay, named by the day it starts. */
    public record Night(LocalDate date, BigDecimal price, boolean weekend) {
    }

    public static RoomQuote of(StayRoom room, Set<DayOfWeek> weekendNights, LocalDate checkIn, LocalDate checkOut,
                               int rooms, int adults, int children) {
        List<Night> nights = new ArrayList<>();
        int weekend = 0;
        BigDecimal sum = BigDecimal.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            boolean dearer = weekendNights.contains(night.getDayOfWeek());
            BigDecimal price = dearer && room.getWeekendPrice() != null ? room.getWeekendPrice() : room.getPricePerNight();
            if (dearer) {
                weekend++;
            }
            if (price != null) {
                sum = sum.add(price);
            }
            nights.add(new Night(night, price, dearer));
        }
        BigDecimal total = room.getPricePerNight() == null ? null
                : sum.multiply(BigDecimal.valueOf(rooms)).setScale(2, RoundingMode.HALF_UP);
        int maxAdults = room.getMaxAdults() == null ? 2 : room.getMaxAdults();
        int maxChildren = room.getMaxChildren() == null ? 0 : room.getMaxChildren();
        int minNights = room.getMinNights() == null ? 1 : room.getMinNights();
        return new RoomQuote(room.getId(), room.getName(), room.getPricePerNight(), room.getWeekendPrice(), nights, weekend, total,
                adults <= maxAdults * rooms && children <= maxChildren * rooms,
                room.getRoomCount() == null || rooms <= room.getRoomCount(),
                nights.size() >= minNights, minNights,
                Boolean.TRUE.equals(room.getBreakfastIncluded()), !Boolean.FALSE.equals(room.getTaxesIncluded()));
    }

    /** Priced, and the guests, the rooms and the nights all fit. */
    public boolean bookable() {
        return total != null && fitsGuests && enoughRooms && meetsMinNights;
    }
}
