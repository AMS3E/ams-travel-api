package ams.asmtravel.api.services.destination.stay.domain;

import java.util.List;

/**
 * Fixed lists for a stay's breakfast, payment and languages, so they can be filtered.
 * The dashboard has the same lists and a test there reads this file.
 */
public final class StayVocabularies {

    public static final List<String> BREAKFAST_TYPES = List.of(
            "khmer", "asian", "continental", "american", "buffet", "vegetarian", "vegan", "halal");

    /** Local methods first. */
    public static final List<String> PAYMENT_METHODS = List.of(
            "cash", "khqr", "aba-pay", "acleda", "wing", "visa", "mastercard", "unionpay", "jcb", "amex",
            "paypal", "bank-transfer");

    /** ISO 639-1 codes. */
    public static final List<String> LANGUAGES = List.of("km", "en", "zh", "fr", "th", "vi", "ko", "ja", "de", "ru", "es");

    private StayVocabularies() {
    }

    /** The values not in {@code allowed}. */
    public static List<String> unknown(List<String> values, List<String> allowed) {
        return values == null ? List.of() : values.stream().filter(one -> !allowed.contains(one)).sorted().toList();
    }
}
