package ams.asmtravel.api.services.destination.stayroom;

import ams.asmtravel.api.services.destination.stayroom.domain.RoomQuote;
import ams.asmtravel.api.services.destination.stayroom.domain.StayRoom;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Friday 16 to Monday 19 October 2026: Friday and Saturday nights are the weekend, Sunday night is not. */
class RoomQuoteTests {

    private static final Set<DayOfWeek> WEEKEND = EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
    private static final LocalDate FRIDAY = LocalDate.of(2026, 10, 16);
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);

    @Test
    void weekendNightsCostTheWeekendPriceAndTheTotalCoversEveryRoomAsked() {
        StayRoom villa = room(new BigDecimal("100"), new BigDecimal("150"));
        RoomQuote quote = RoomQuote.of(villa, WEEKEND, FRIDAY, MONDAY, 2, 4, 0);
        assertThat(quote.nights()).hasSize(3);
        assertThat(quote.weekendNightCount()).isEqualTo(2);
        assertThat(quote.nights().get(0).weekend()).isTrue();
        assertThat(quote.nights().get(2).weekend()).isFalse();
        assertThat(quote.total()).isEqualByComparingTo("800.00");
        assertThat(quote.bookable()).isTrue();
    }

    @Test
    void withoutAWeekendPriceEveryNightCostsTheNightlyPrice() {
        RoomQuote quote = RoomQuote.of(room(new BigDecimal("100"), null), WEEKEND, FRIDAY, MONDAY, 1, 2, 0);
        assertThat(quote.weekendNightCount()).isEqualTo(2);
        assertThat(quote.total()).isEqualByComparingTo("300.00");
    }

    @Test
    void tooFewNightsTooManyGuestsOrNoPriceMakeItNotBookable() {
        StayRoom villa = room(new BigDecimal("100"), new BigDecimal("150"));
        villa.setMinNights(2);
        assertThat(RoomQuote.of(villa, WEEKEND, FRIDAY, FRIDAY.plusDays(1), 1, 2, 0).meetsMinNights()).isFalse();
        assertThat(RoomQuote.of(villa, WEEKEND, FRIDAY, MONDAY, 1, 3, 0).fitsGuests()).isFalse();
        assertThat(RoomQuote.of(villa, WEEKEND, FRIDAY, MONDAY, 4, 2, 0).enoughRooms()).isFalse();
        villa.setPricePerNight(null);
        RoomQuote unpriced = RoomQuote.of(villa, WEEKEND, FRIDAY, MONDAY, 1, 2, 0);
        assertThat(unpriced.total()).isNull();
        assertThat(unpriced.bookable()).isFalse();
    }

    private static StayRoom room(BigDecimal nightly, BigDecimal weekend) {
        StayRoom room = new StayRoom();
        room.setName("Garden Villa");
        room.setPricePerNight(nightly);
        room.setWeekendPrice(weekend);
        room.setMaxAdults(2);
        room.setMaxChildren(1);
        room.setRoomCount(3);
        room.setMinNights(1);
        return room;
    }
}
