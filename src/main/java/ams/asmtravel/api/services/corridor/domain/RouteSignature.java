package ams.asmtravel.api.services.corridor.domain;

import ams.asmtravel.api.services.destination.destination.domain.Destination;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** Hash of stops, positions and transports, to tell if a stored route still fits without calling the router. */
public final class RouteSignature {

    /** Bump when leg logic changes: every stored route becomes stale and is recalculated on startup. */
    private static final String VERSION = "v2;";

    private RouteSignature() {
    }

    public static String of(List<CorridorStop> stopsInOrder) {
        StringBuilder text = new StringBuilder(VERSION);
        for (int index = 0; index < stopsInOrder.size(); index++) {
            CorridorStop stop = stopsInOrder.get(index);
            text.append(place(stop.getDestination())).append('|')
                    .append(index == 0 ? "" : CorridorTransportType.orDefault(stop.getTransportType()).name())
                    .append(';');
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is always available", impossible);
        }
    }

    /** Compares the stored route with the current stops. */
    public static CorridorRouteStatus status(Corridor corridor, List<CorridorStop> stopsInOrder) {
        if (stopsInOrder.size() < 2) {
            return CorridorRouteStatus.NONE;
        }
        if (corridor.getRouteSignature() == null) {
            return CorridorRouteStatus.MISSING;
        }
        return corridor.getRouteSignature().equals(of(stopsInOrder)) ? CorridorRouteStatus.CURRENT : CorridorRouteStatus.STALE;
    }

    /** Known legs by key, to reuse after the stops are rewritten. */
    public static Map<String, CorridorLeg> legsOf(List<CorridorStop> stopsInOrder) {
        Map<String, CorridorLeg> legs = new HashMap<>();
        for (int index = 1; index < stopsInOrder.size(); index++) {
            CorridorStop stop = stopsInOrder.get(index);
            if (stop.hasLeg()) {
                legs.put(legKey(stopsInOrder.get(index - 1), stop), stop.leg());
            }
        }
        return legs;
    }

    /**
     * Puts known legs back on rewritten stops where places and transport are unchanged.
     *
     * @return true if every leg was known (no routing call needed)
     */
    public static boolean restoreLegs(List<CorridorStop> stopsInOrder, Map<String, CorridorLeg> known) {
        boolean complete = stopsInOrder.size() >= 2;
        for (int index = 0; index < stopsInOrder.size(); index++) {
            CorridorLeg leg = index == 0 ? null : known.get(legKey(stopsInOrder.get(index - 1), stopsInOrder.get(index)));
            stopsInOrder.get(index).setLeg(leg);
            complete &= index == 0 || leg != null;
        }
        return complete;
    }

    private static String legKey(CorridorStop from, CorridorStop to) {
        return place(from.getDestination()) + ">" + place(to.getDestination()) + "|"
                + CorridorTransportType.orDefault(to.getTransportType()).name();
    }

    private static String place(Destination destination) {
        return destination.getId() + "@" + number(destination.getLatitude()) + "," + number(destination.getLongitude());
    }

    private static String number(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }
}
