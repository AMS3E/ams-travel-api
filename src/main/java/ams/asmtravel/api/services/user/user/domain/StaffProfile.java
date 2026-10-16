package ams.asmtravel.api.services.user.user.domain;

/** What the API keeps about a staff account beyond Keycloak: the phone and photo on the person's profile here. */
public record StaffProfile(String phone, String avatarKey) {
}
