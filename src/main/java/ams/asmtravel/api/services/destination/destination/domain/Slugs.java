package ams.asmtravel.api.services.destination.destination.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** URL names of rows, like {@code angkor-wat}: lower-case ASCII words joined by hyphens. */
public final class Slugs {

    /** For {@code @Pattern} on request fields. */
    public static final String PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$";

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NOT_WORD = Pattern.compile("[^a-z0-9]+");
    private static final int MAX_LENGTH = 100;

    private Slugs() {
    }

    /**
     * A slug from a name: accents dropped, other non-letters become one hyphen.
     *
     * @throws org.springframework.web.server.ResponseStatusException 400 when nothing is left (e.g. a Khmer-only name)
     */
    public static String of(String name) {
        String ascii = MARKS.matcher(Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD))
                .replaceAll("");
        String slug = NOT_WORD.matcher(ascii.toLowerCase(Locale.ROOT)).replaceAll("-").replaceAll("^-+|-+$", "");
        if (slug.length() > MAX_LENGTH) {
            slug = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
        }
        if (slug.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Give this a slug: its name has no letters a URL can use");
        }
        return slug;
    }

    /** {@code wanted}, or when taken {@code wanted-2}, {@code wanted-3}… */
    public static String unique(String wanted, Predicate<String> taken) {
        if (!taken.test(wanted)) {
            return wanted;
        }
        for (int suffix = 2; ; suffix++) {
            String candidate = wanted + "-" + suffix;
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
    }

    /**
     * The slug asked for (must be free), or a free one made from the name.
     *
     * @throws org.springframework.web.server.ResponseStatusException 409 when the slug asked for is taken
     */
    public static String forCreate(String wanted, String name, Predicate<String> taken) {
        if (wanted != null && !wanted.isBlank()) {
            if (taken.test(wanted)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "The slug " + wanted + " is already used");
            }
            return wanted;
        }
        return unique(of(name), taken);
    }

    /** An edit keeps its slug unless it sends a different, free one. */
    public static String forUpdate(String wanted, String current, Predicate<String> takenByAnother) {
        if (wanted == null || wanted.isBlank() || wanted.equals(current)) {
            return current;
        }
        if (takenByAnother.test(wanted)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The slug " + wanted + " is already used");
        }
        return wanted;
    }
}
