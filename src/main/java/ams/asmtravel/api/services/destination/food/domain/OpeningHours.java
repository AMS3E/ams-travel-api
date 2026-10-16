package ams.asmtravel.api.services.destination.food.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A food place's week, as staff type it: day ({@code mon}…{@code sun}) to hours ({@code 07:00-22:00}),
 * {@code closed} or nothing on a day it is shut. Read in Cambodia's time, whatever the server's clock says.
 * A close before its open ({@code 18:00-02:00}) runs past midnight, so the place is still open
 * in the small hours of the next day; {@code 24:00} means midnight.
 */
public final class OpeningHours {

    public static final ZoneId CAMBODIA = ZoneId.of("Asia/Phnom_Penh");

    private static final List<String> DAYS = List.of("mon", "tue", "wed", "thu", "fri", "sat", "sun");

    private OpeningHours() {
    }

    /** Whether the place is open at this moment in Cambodia; null when no hours are known at all. */
    public static Boolean openNow(Map<String, String> hours) {
        return openAt(hours, ZonedDateTime.now(CAMBODIA));
    }

    /** Today's line as staff typed it ({@code 07:00-22:00}), {@code closed} on a day it is shut, null when no hours are known. */
    public static String today(Map<String, String> hours) {
        return todayAt(hours, ZonedDateTime.now(CAMBODIA));
    }

    public static Boolean openAt(Map<String, String> hours, ZonedDateTime at) {
        if (hours == null || hours.isEmpty()) {
            return null;
        }
        ZonedDateTime local = at.withZoneSameInstant(CAMBODIA);
        LocalTime now = local.toLocalTime();
        Range today = parse(hours.get(key(local.getDayOfWeek())));
        if (today != null && today.covers(now)) {
            return true;
        }
        Range yesterday = parse(hours.get(key(local.getDayOfWeek().minus(1))));
        return yesterday != null && yesterday.pastMidnight() && now.isBefore(yesterday.close());
    }

    public static String todayAt(Map<String, String> hours, ZonedDateTime at) {
        if (hours == null || hours.isEmpty()) {
            return null;
        }
        String line = hours.get(key(at.withZoneSameInstant(CAMBODIA).getDayOfWeek()));
        return line == null || line.isBlank() || line.trim().equalsIgnoreCase("closed") ? "closed" : line.trim();
    }

    private static String key(DayOfWeek day) {
        return DAYS.get(day.getValue() - 1);
    }

    /** {@code HH:mm-HH:mm}; anything else (closed, blank, free text) is a shut day. */
    private static Range parse(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.trim().toLowerCase(Locale.ROOT).split("-");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new Range(time(parts[0]), time(parts[1]));
        } catch (RuntimeException notATime) {
            return null;
        }
    }

    private static LocalTime time(String text) {
        String clean = text.trim();
        return clean.equals("24:00") ? LocalTime.MIDNIGHT : LocalTime.parse(clean);
    }

    private record Range(LocalTime open, LocalTime close) {

        boolean pastMidnight() {
            return !close.isAfter(open);
        }

        boolean covers(LocalTime now) {
            return pastMidnight() ? !now.isBefore(open) || now.isBefore(close) : !now.isBefore(open) && now.isBefore(close);
        }
    }
}
