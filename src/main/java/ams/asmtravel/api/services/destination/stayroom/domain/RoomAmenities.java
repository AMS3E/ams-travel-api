package ams.asmtravel.api.services.destination.stayroom.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Room amenities as a fixed list (the property's own are in {@code StayAmenities}).
 * The dashboard has the same list and a test there reads this file.
 */
public final class RoomAmenities {

    /** Group heading to slugs, in display order. */
    public static final Map<String, List<String>> GROUPS = Map.ofEntries(
            Map.entry("Comfort", List.of("air-conditioning", "fan", "heating", "mosquito-net", "blackout-curtains", "soundproofing",
                    "privacy-curtain", "reading-light")),
            Map.entry("Bathroom", List.of("hot-water", "shower", "rain-shower", "bathtub", "bidet", "hairdryer", "toiletries",
                    "towels", "slippers", "bathrobes")),
            Map.entry("Food & drink", List.of("kettle", "coffee-machine", "minibar", "fridge", "kitchenette", "kitchen",
                    "microwave", "water-bottles")),
            Map.entry("Tech & work", List.of("wifi", "tv", "streaming", "desk", "usb-sockets", "phone")),
            Map.entry("Space & outdoors", List.of("balcony", "terrace", "private-pool", "seating-area", "wardrobe", "iron",
                    "clothes-rack")),
            Map.entry("Safety & access", List.of("safe", "lockers", "smoke-alarm", "step-free", "accessible-bathroom", "ground-floor")),
            Map.entry("Family", List.of("cot-available", "extra-bed-available")));

    /** Every known slug, for validation. */
    public static final Set<String> ALL = GROUPS.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toUnmodifiableSet());

    private RoomAmenities() {
    }

    /** The given slugs that are not known. */
    public static List<String> unknown(List<String> amenities) {
        return amenities == null ? List.of() : amenities.stream().filter(one -> !ALL.contains(one)).sorted().toList();
    }
}
