package ams.asmtravel.api.services.user.user.domain;

/**
 * How complete a traveller's profile is, for the "80% profile completed" line: five fields count for a
 * fifth each: name, email, phone, picture and home province. The server owns the formula so every
 * client shows the same number.
 */
public final class ProfileCompletion {

    private static final int FIELDS = 5;

    private ProfileCompletion() {
    }

    public static int of(User user) {
        int filled = 0;
        filled += has(user.getFullName()) ? 1 : 0;
        filled += has(user.getEmail()) ? 1 : 0;
        filled += has(user.getPhone()) ? 1 : 0;
        filled += has(user.getAvatarKey()) ? 1 : 0;
        filled += user.getHomeProvinceId() != null ? 1 : 0;
        return filled * 100 / FIELDS;
    }

    private static boolean has(String value) {
        return value != null && !value.isBlank();
    }
}
