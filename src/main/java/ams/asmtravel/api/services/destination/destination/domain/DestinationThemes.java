package ams.asmtravel.api.services.destination.destination.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An attraction's theme (main tourism region code) and the grouped features only that theme has.
 * The dashboard has the same lists and a test there reads this file.
 */
public final class DestinationThemes {

    /** Theme, then group heading, then slugs. */
    public static final Map<String, Map<String, List<String>>> BY_THEME = Map.ofEntries(
            /* None: ancient sites use heritage tags instead. */
            Map.entry("ANCIENT-CAPITALS", Map.of()),
            Map.entry("NORTHEASTERN", Map.ofEntries(
                    Map.entry("Nature", List.of("crater-lake", "primary-forest", "waterfall-pools", "wildlife-watching", "elephant-sanctuary")),
                    Map.entry("People", List.of("indigenous-village", "community-guides", "village-handicrafts", "coffee-plantation")),
                    Map.entry("Getting in", List.of("dry-season-only", "dirt-road", "guide-required", "river-crossing")))),
            Map.entry("MEKONG-TONLE-SAP", Map.ofEntries(
                    Map.entry("On the water", List.of("boat-trip", "floating-houses", "dolphin-watching", "birds-from-the-boat", "river-sunset")),
                    Map.entry("Seasons", List.of("high-water", "low-water", "water-festival")),
                    Map.entry("The village", List.of("fishing-village", "silk-weaving", "community-homestay", "community-fee")))),
            Map.entry("MOUNTAIN-WATERFALL", Map.ofEntries(
                    Map.entry("Trail", List.of("trail-easy", "trail-moderate", "trail-hard", "trail-signed", "ranger-station")),
                    Map.entry("Water", List.of("swimming-allowed", "wet-season-best", "natural-pool")),
                    Map.entry("Views & nights", List.of("sunrise-point", "sea-of-clouds", "camping-allowed", "cool-climate")))),
            Map.entry("COASTAL-ISLAND", Map.ofEntries(
                    Map.entry("The beach", List.of("white-sand", "calm-water", "safe-swimming", "shade-trees", "sunset-beach", "quiet-beach")),
                    Map.entry("In the water", List.of("snorkel-reef", "dive-sites", "kayak-hire", "bioluminescence")),
                    Map.entry("Getting to it", List.of("by-ferry", "by-speedboat", "by-road")),
                    Map.entry("On the beach", List.of("beach-bars", "sun-loungers", "lifeguard")))),
            Map.entry("URBAN-LIFESTYLE", Map.ofEntries(
                    Map.entry("What it is", List.of("city-landmark", "colonial-architecture", "art-space", "rooftop-view", "market-hall")),
                    Map.entry("When to go", List.of("best-by-day", "best-at-night", "weekends-only", "open-late")),
                    Map.entry("Getting around town", List.of("easy-on-foot", "short-tuk-tuk")))),
            Map.entry("KHMER-CULINARY", Map.ofEntries(
                    Map.entry("Taste", List.of("signature-dish", "tasting-flight", "palm-sugar-making", "pepper-tasting", "rice-wine-tasting")),
                    Map.entry("Learn", List.of("cooking-class-onsite", "farm-tour", "market-tour")),
                    Map.entry("Planning", List.of("book-ahead", "good-for-families")))),
            Map.entry("ECO-COMMUNITY", Map.ofEntries(
                    Map.entry("The community", List.of("community-run", "homestay-nights", "local-guides", "craft-workshop", "farm-life")),
                    Map.entry("How you help", List.of("fees-to-village", "conservation-project", "volunteer-days")),
                    Map.entry("Reaching the village", List.of("by-bicycle", "by-boat", "remote-village")))),
            Map.entry("LUXURY-TOURISM", Map.ofEntries(
                    Map.entry("Private", List.of("private-guide-service", "private-transfer", "helicopter-access", "yacht-charter", "vip-entry")),
                    Map.entry("Comfort", List.of("spa-onsite", "fine-dining-onsite", "golf-course", "butler-service")),
                    Map.entry("Booking", List.of("by-appointment", "exclusive-use")))));

    /** Every known slug, for validation. */
    public static final Set<String> ALL = BY_THEME.values().stream()
            .flatMap(groups -> groups.values().stream())
            .flatMap(List::stream)
            .collect(Collectors.toUnmodifiableSet());

    private DestinationThemes() {
    }

    public static boolean isTheme(String code) {
        return code != null && BY_THEME.containsKey(code);
    }

    /** The given slugs that are not known. */
    public static List<String> unknown(List<String> specialties) {
        return specialties == null ? List.of() : specialties.stream().filter(one -> !ALL.contains(one)).sorted().toList();
    }
}
