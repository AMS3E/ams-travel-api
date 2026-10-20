package ams.asmtravel.api.services.destination.destination.domain;

/** Rules for the hand-typed Khmer text beside the English. */
public final class KhmerText {

    private KhmerText() {
    }

    /** True when the English says something and the Khmer does not. */
    public static boolean lacks(String english, String khmer) {
        return hasText(english) && !hasText(khmer);
    }

    /** Rich text counts only its words: an empty paragraph is blank. */
    public static boolean hasText(String value) {
        return value != null && !plain(value).isBlank();
    }

    /** The first words of rich text as plain text, cut at {@code max} characters; null when there are none. */
    public static String excerpt(String value, int max) {
        if (!hasText(value)) {
            return null;
        }
        String text = plain(value).replaceAll("\\s+", " ").trim();
        return text.length() <= max ? text : text.substring(0, max).trim() + "…";
    }

    /** Rich text is Markdown (older rows may hold HTML): links keep their words, marks and tags go. */
    private static String plain(String value) {
        return value.replaceAll("<[^>]*>", " ").replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
                .replaceAll("!?\\[([^\\]]*)]\\([^)]*\\)", "$1")
                .replaceAll("(?m)^\\s*(?:[-+*]|\\d+[.)]|#{1,6}|>)\\s+", "")
                .replaceAll("[*_~`]+", "");
    }
}
