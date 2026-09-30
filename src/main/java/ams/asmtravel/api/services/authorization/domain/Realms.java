package ams.asmtravel.api.services.authorization.domain;

/** The two Keycloak realms this API trusts. */
public final class Realms {

    /** Customers of the public website. */
    public static final String CUSTOMER = "amstravel";

    /** Staff: all desks. */
    public static final String BACKOFFICE = "amsadmin";

    private Realms() {
    }
}
