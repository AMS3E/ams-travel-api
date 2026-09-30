package ams.asmtravel.api.services.user.user.domain;

/** Whether a person may sign in. INACTIVE also disables their customer sign-in in Keycloak. */
public enum UserStatus {
    ACTIVE,
    INACTIVE
}
