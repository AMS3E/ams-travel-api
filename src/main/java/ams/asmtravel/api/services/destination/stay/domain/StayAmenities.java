package ams.asmtravel.api.services.destination.stay.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stay amenities as a fixed, filterable list of slugs; groups are for display only.
 * The dashboard has the same list and a test there reads this file.
 */
public final class StayAmenities {

    /** Group heading to slugs, in display order. */
    public static final Map<String, List<String>> GROUPS = Map.ofEntries(
            Map.entry("Internet", List.of("wifi", "wifi-free", "workspace")),
            Map.entry("Parking & transport", List.of("parking", "parking-free", "airport-shuttle", "car-hire")),
            Map.entry("Wellbeing", List.of("pool", "spa", "gym", "garden", "terrace")),
            Map.entry("Food & drink", List.of("breakfast", "restaurant", "bar", "room-service", "kitchen")),
            Map.entry("Room", List.of("air-conditioning", "heating", "private-bathroom", "balcony", "tv", "safe")),
            Map.entry("Services", List.of("front-desk-24h", "laundry", "housekeeping", "luggage-storage", "tours")),
            Map.entry("Family & pets", List.of("family-rooms", "cot", "pets-allowed")),
            Map.entry("Accessibility", List.of("step-free-access", "lift", "accessible-bathroom")));

    /** Every known slug, for validation. */
    public static final Set<String> ALL = GROUPS.values().stream()
            .flatMap(List::stream)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

    private StayAmenities() {
    }

    /** The given slugs that are not known. */
    public static List<String> unknown(List<String> amenities) {
        return amenities == null ? List.of() : amenities.stream().filter(one -> !ALL.contains(one)).sorted().toList();
    }
}
