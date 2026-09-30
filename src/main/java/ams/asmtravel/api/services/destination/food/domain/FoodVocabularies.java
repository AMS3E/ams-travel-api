package ams.asmtravel.api.services.destination.food.domain;

import java.util.List;

/**
 * Fixed word lists a food listing chooses from, so they can be filtered.
 * The dashboard has the same lists and a test there reads this file.
 */
public final class FoodVocabularies {

    /** Cambodian first. */
    public static final List<String> CUISINES = List.of(
            "khmer", "street-food", "seafood", "bbq-grill", "noodles", "rice-dishes", "soup",
            "chinese", "vietnamese", "thai", "indian", "japanese", "korean", "malay-indonesian",
            "western", "french", "italian", "mexican", "middle-eastern", "fusion",
            "vegetarian", "bakery-pastry", "desserts", "coffee", "juice-smoothies", "cocktails");

    public static final List<String> MEALS = List.of(
            "breakfast", "brunch", "lunch", "dinner", "late-night", "snacks", "coffee-break");

    public static final List<String> DIETARY = List.of(
            "vegetarian-options", "vegan-options", "halal", "no-pork", "gluten-free",
            "nut-free-on-request", "less-spicy-on-request", "msg-free-on-request");

    public static final List<String> SERVICE_OPTIONS = List.of(
            "dine-in", "takeaway", "delivery", "table-service", "counter-order", "self-service",
            "buffet", "catering", "private-dining");

    public static final List<String> GOOD_FOR = List.of(
            "families", "groups", "couples", "solo", "business", "quick-bite", "long-lunch",
            "sunset", "late-night", "vegetarians", "special-occasion", "trying-khmer-food");

    /** Delivery apps in Cambodia. */
    public static final List<String> DELIVERY_PARTNERS = List.of(
            "foodpanda", "nham24", "grab", "muuve", "own-delivery");

    /** Local methods first. */
    public static final List<String> PAYMENT_METHODS = List.of(
            "cash", "khqr", "aba-pay", "acleda", "wing", "visa", "mastercard", "unionpay", "jcb", "amex",
            "paypal", "bank-transfer");

    /** ISO 639-1 codes for staff and menu languages. */
    public static final List<String> LANGUAGES = List.of("km", "en", "zh", "fr", "th", "vi", "ko", "ja", "de", "ru", "es");

    /** Labels on a dish. */
    public static final List<String> DISH_LABELS = List.of(
            "vegetarian", "vegan", "halal", "gluten-free", "raw",
            "contains-pork", "contains-nuts", "contains-shellfish", "contains-dairy");

    private FoodVocabularies() {
    }

    /** The values not in {@code allowed}. */
    public static List<String> unknown(List<String> values, List<String> allowed) {
        return values == null ? List.of() : values.stream().filter(one -> !allowed.contains(one)).sorted().toList();
    }
}
