package ams.asmtravel.api.services.destination.stay.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Grouped features only one kind of stay has. A stay keeps slugs from another kind after a type change.
 * The dashboard has the same lists and a test there reads this file.
 */
public final class StaySpecialties {

    /** Kind, then group heading, then slugs. */
    public static final Map<String, Map<String, List<String>>> BY_KIND = Map.ofEntries(
            Map.entry("HOTEL", Map.ofEntries(
                    Map.entry("Kind of hotel", List.of("boutique-hotel", "business-hotel", "heritage-building", "riverfront-hotel", "airport-hotel")),
                    Map.entry("At the front desk", List.of("concierge", "porter", "express-check-in", "currency-exchange", "sim-cards", "visa-help")),
                    Map.entry("Meetings & events", List.of("meeting-rooms", "business-centre", "event-hall", "executive-lounge")))),
            Map.entry("GUESTHOUSE", Map.ofEntries(
                    Map.entry("The house", List.of("family-run", "hosts-on-site", "guest-kitchen", "common-room", "rooftop-terrace")),
                    Map.entry("The hosts can arrange", List.of("tuk-tuk-drivers", "bus-boat-tickets", "bicycle-hire", "motorbike-hire", "visa-extensions")),
                    Map.entry("Kind of guesthouse", List.of("budget-guesthouse", "colonial-house", "garden-bungalows")))),
            Map.entry("HOSTEL", Map.ofEntries(
                    Map.entry("Beds", List.of("mixed-dorms", "female-dorms", "pod-beds", "private-rooms-too")),
                    Map.entry("Social life", List.of("party-hostel", "quiet-hostel", "shared-dinners", "pub-crawls", "free-walking-tours")),
                    Map.entry("For backpackers", List.of("hostel-kitchen", "travel-desk", "book-exchange", "coworking-corner")))),
            Map.entry("RESORT", Map.ofEntries(
                    Map.entry("Setting", List.of("beachfront", "private-island", "jungle-setting", "riverside-setting", "hills-setting")),
                    Map.entry("Meal plans", List.of("all-inclusive", "full-board", "half-board")),
                    Map.entry("Activities", List.of("kids-club", "water-sports", "diving", "kayaks", "yoga", "nature-walks")),
                    Map.entry("Getting there", List.of("boat-transfer", "resort-shuttle")))),
            Map.entry("HOMESTAY", Map.ofEntries(
                    Map.entry("The home", List.of("stilt-house", "village-setting", "rice-field-view", "meals-with-family")),
                    Map.entry("Join in", List.of("cook-with-hosts", "farm-work", "village-crafts", "fishing-trips", "ox-cart-rides", "village-walks")),
                    Map.entry("Community", List.of("community-tourism", "money-to-village")))),
            Map.entry("VILLA", Map.ofEntries(
                    Map.entry("The villa", List.of("whole-villa", "villa-pool", "villa-garden", "villa-bbq", "villa-kitchen")),
                    Map.entry("Staff", List.of("housekeeper", "private-chef", "driver", "night-guard")),
                    Map.entry("Where the villa is", List.of("villa-beach", "villa-countryside", "villa-town")))));

    /** Every known slug, for validation. */
    public static final Set<String> ALL = BY_KIND.values().stream()
            .flatMap(groups -> groups.values().stream())
            .flatMap(List::stream)
            .collect(Collectors.toUnmodifiableSet());

    private StaySpecialties() {
    }

    /** The given slugs that are not known. */
    public static List<String> unknown(List<String> specialties) {
        return specialties == null ? List.of() : specialties.stream().filter(one -> !ALL.contains(one)).sorted().toList();
    }
}
