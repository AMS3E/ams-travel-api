package ams.asmtravel.api.services.destination.food.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Grouped features only one kind of food place has. A listing keeps slugs from another kind after a type change.
 * The dashboard has the same lists and a test there reads this file.
 */
public final class FoodSpecialties {

    /** Kind, then group heading, then slugs, in display order. */
    public static final Map<String, Map<String, List<String>>> BY_KIND = Map.ofEntries(
            Map.entry("CAFE", Map.ofEntries(
                    Map.entry("Coffee", List.of("khmer-iced-coffee", "espresso", "cappuccino-latte", "pour-over", "cold-brew")),
                    Map.entry("Other drinks", List.of("tea", "matcha", "smoothies", "fresh-juice", "fresh-coconut")),
                    Map.entry("Beans", List.of("mondulkiri-beans", "ratanakiri-beans", "imported-beans", "roasted-in-house")),
                    Map.entry("Milk", List.of("condensed-milk", "fresh-milk", "oat-milk", "soy-milk", "almond-milk")),
                    Map.entry("Baked here", List.of("bread", "baguettes", "pastries", "cakes", "khmer-sweets", "cookies", "savoury-pies")),
                    Map.entry("Orders", List.of("cakes-to-order", "whole-cakes", "party-trays")),
                    Map.entry("Freshness", List.of("baked-every-morning", "baked-twice-a-day")))),
            Map.entry("STREET_FOOD", Map.ofEntries(
                    Map.entry("Cooked on", List.of("charcoal-grill", "wok", "steamer", "broth-pot", "griddle", "deep-fryer")),
                    Map.entry("Sold from", List.of("cart", "stall", "motorbike-cart", "night-market-pitch")),
                    Map.entry("Selling times", List.of("mornings-only", "evenings-only", "all-day")))),
            Map.entry("MARKET", Map.ofEntries(
                    Map.entry("Kind of market", List.of("day-market", "night-market", "wet-market", "food-hall", "floating-market")),
                    Map.entry("Also sells", List.of("fresh-produce", "souvenirs", "clothes", "crafts", "spices")),
                    Map.entry("Size", List.of("under-20-stalls", "20-to-50-stalls", "over-50-stalls")))),
            Map.entry("RESTAURANT", Map.ofEntries(
                    Map.entry("How it is served", List.of("family-style", "a-la-carte", "set-menu", "tasting-menu", "cook-at-table")),
                    Map.entry("Cooking", List.of("wood-fired", "chefs-table", "khmer-home-cooking", "royal-khmer-recipes",
                            "live-seafood", "own-garden")),
                    Map.entry("Groups & events", List.of("banquets", "group-set-menus", "birthday-parties", "cooking-classes")),
                    Map.entry("Drinks", List.of("draught-beer", "craft-beer", "signature-cocktails", "wine", "spirits", "khmer-rice-wine", "mocktails")),
                    Map.entry("Nights", List.of("happy-hour", "live-band", "dj-nights", "sports-screenings", "quiz-night")),
                    Map.entry("Dress code", List.of("dress-casual", "dress-smart-casual", "dress-formal", "over-18-only", "smart-dress")))));

    /** Every known slug, for validation. */
    public static final Set<String> ALL = BY_KIND.values().stream()
            .flatMap(groups -> groups.values().stream())
            .flatMap(List::stream)
            .collect(Collectors.toUnmodifiableSet());

    private FoodSpecialties() {
    }

    /** The given slugs that are not known. */
    public static List<String> unknown(List<String> specialties) {
        return specialties == null ? List.of() : specialties.stream().filter(one -> !ALL.contains(one)).sorted().toList();
    }
}
