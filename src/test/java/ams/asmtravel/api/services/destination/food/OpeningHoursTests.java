package ams.asmtravel.api.services.destination.food;

import ams.asmtravel.api.services.destination.food.domain.OpeningHours;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** The week as staff type it, read in Cambodia's time. */
class OpeningHoursTests {

    /** Wednesday 14 October 2026, 10:30 in Phnom Penh (UTC+7). */
    private static final ZonedDateTime WEDNESDAY_MORNING = ZonedDateTime.of(2026, 10, 14, 10, 30, 0, 0, OpeningHours.CAMBODIA);

    @Test
    void openInsideTheHoursClosedOutsideThem() {
        Map<String, String> hours = Map.of("wed", "07:00-22:00", "thu", "closed");
        assertThat(OpeningHours.openAt(hours, WEDNESDAY_MORNING)).isTrue();
        assertThat(OpeningHours.openAt(hours, WEDNESDAY_MORNING.withHour(23))).isFalse();
        assertThat(OpeningHours.openAt(hours, WEDNESDAY_MORNING.plusDays(1))).isFalse();
        assertThat(OpeningHours.todayAt(hours, WEDNESDAY_MORNING)).isEqualTo("07:00-22:00");
        assertThat(OpeningHours.todayAt(hours, WEDNESDAY_MORNING.plusDays(1))).isEqualTo("closed");
        assertThat(OpeningHours.todayAt(hours, WEDNESDAY_MORNING.plusDays(2))).isEqualTo("closed");
    }

    @Test
    void aNightRunsPastMidnightIntoTheNextDay() {
        Map<String, String> hours = Map.of("tue", "18:00-02:00");
        assertThat(OpeningHours.openAt(hours, WEDNESDAY_MORNING.withHour(1))).isTrue();
        assertThat(OpeningHours.openAt(hours, WEDNESDAY_MORNING.withHour(3))).isFalse();
        assertThat(OpeningHours.openAt(hours, WEDNESDAY_MORNING.minusDays(1).withHour(20))).isTrue();
        assertThat(OpeningHours.openAt(Map.of("wed", "00:00-24:00"), WEDNESDAY_MORNING.withHour(23).withMinute(59))).isTrue();
    }

    @Test
    void theClockIsCambodias() {
        // 03:30 UTC on Wednesday is 10:30 in Phnom Penh: open, whatever zone the caller holds the moment in.
        assertThat(OpeningHours.openAt(Map.of("wed", "07:00-22:00"), WEDNESDAY_MORNING.withZoneSameInstant(ZoneOffset.UTC))).isTrue();
    }

    @Test
    void noHoursMeansNothingIsKnown() {
        assertThat(OpeningHours.openAt(Map.of(), WEDNESDAY_MORNING)).isNull();
        assertThat(OpeningHours.openAt(null, WEDNESDAY_MORNING)).isNull();
        assertThat(OpeningHours.todayAt(Map.of(), WEDNESDAY_MORNING)).isNull();
        // Free text nobody can read is a shut day, not an error.
        assertThat(OpeningHours.openAt(Map.of("wed", "whenever"), WEDNESDAY_MORNING)).isFalse();
    }
}
