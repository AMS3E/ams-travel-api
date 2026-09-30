package ams.asmtravel.api.services.destination.food.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Food place amenities as a fixed list of slugs.
 * The dashboard has the same list and a test there reads this file.
 */
public final class FoodAmenities {

    /** Group heading to slugs, in display order. */
    public static final Map<String, List<String>> GROUPS = Map.ofEntries(
            Map.entry("Seating", List.of("air-conditioned", "outdoor-seating", "rooftop", "riverside", "garden",
                    "street-side", "floor-seating", "private-room")),
            Map.entry("While you eat", List.of("wifi", "toilets", "power-sockets", "live-music", "tv-sport",
                    "non-smoking", "smoking-area", "pet-friendly")),
            Map.entry("Family", List.of("high-chairs", "kids-menu", "play-area", "baby-changing")),
            Map.entry("Access", List.of("step-free", "accessible-toilet", "ground-floor")),
            Map.entry("Getting there", List.of("car-parking", "motorbike-parking", "tuk-tuk-stand")),
            Map.entry("Good to know", List.of("picture-menu", "halal-kitchen", "free-drinking-water",
                    "takeaway-boxes", "open-kitchen")));

    /** Every known slug, for validation. */
    public static final Set<String> ALL = GROUPS.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toUnmodifiableSet());

    private FoodAmenities() {
    }

    /** The given slugs that are not known. */
    public static List<String> unknown(List<String> amenities) {
        return amenities == null ? List.of() : amenities.stream().filter(one -> !ALL.contains(one)).sorted().toList();
    }
}
