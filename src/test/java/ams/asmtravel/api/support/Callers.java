package ams.asmtravel.api.support;

import ams.asmtravel.api.services.authorization.domain.Permissions;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Signed-in callers for MockMvc, shaped like the tokens Keycloak issues.
 *
 * <p>RECONSTRUCTED on 2026-09-30: this package was never committed with the
 * rest of the test suite (14 test classes import it). Built from how those
 * tests call it and from the private helpers a few tests define for
 * themselves (see {@code DestinationIntegrationTests}, {@code UserIntegrationTests}).
 * Replace with the original if it turns up.
 *
 * <p>A desk caller carries its realm role plus the permissions that desk's
 * Keycloak composite grants ({@link Permissions#deskComposites()}), exactly as
 * a real token's {@code realm_access.roles} would after composite expansion.
 * A visitor carries only {@code USER}. Authorities are set directly, as the
 * resource server's converter would derive them.
 */
public final class Callers {

    private static final String CUSTOMER_ISSUER = "http://localhost:9090/realms/amstravel";
    private static final String BACKOFFICE_ISSUER = "http://localhost:9090/realms/amsadmin";

    private Callers() {
    }

    /* --- visitors (customer realm) ------------------------------------------------ */

    public static RequestPostProcessor customer() {
        return customer("visitor@visit.test");
    }

    /** A visitor whose subject follows from the email, so the same email is the same person. */
    public static RequestPostProcessor customer(String email) {
        return customer("subject-" + email, email);
    }

    public static RequestPostProcessor customer(String subject, String email) {
        return jwt()
                .jwt(token -> token.issuer(CUSTOMER_ISSUER).subject(subject)
                        .claim("email", email).claim("name", "Visitor " + localPart(email)).claim("preferred_username", email))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    /* --- desks (back-office realm) ------------------------------------------------ */

    /** A staff member with exactly these realm roles: the desk first, then any {@code PERM_*} grants. */
    public static RequestPostProcessor staff(String email, String... roles) {
        return staff("subject-" + email, email, roles);
    }

    public static RequestPostProcessor staff(String subject, String email, String[] roles) {
        List<GrantedAuthority> authorities = Arrays.stream(roles)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
        return jwt()
                .jwt(token -> token.issuer(BACKOFFICE_ISSUER).subject(subject)
                        .claim("email", email).claim("name", "Staff " + localPart(email)).claim("preferred_username", email))
                .authorities(authorities);
    }

    /** An editor as the realm grants one: reviews contributors, publishes, moderates. */
    public static RequestPostProcessor editor() {
        return desk("editor@desk.test", Permissions.EDITOR);
    }

    /** A contributor as the realm grants one: own drafts and listings, sends work to an editor. */
    public static RequestPostProcessor contributor(String email) {
        return desk(email, Permissions.CONTRIBUTOR);
    }

    /** The admin: every desk's permissions plus full control. */
    public static RequestPostProcessor owner() {
        List<String> roles = new ArrayList<>();
        roles.add(Permissions.ADMIN);
        roles.addAll(Permissions.realmRoles());
        return staff("owner@desk.test", roles.toArray(String[]::new));
    }

    /** The realm's display name for a test account: "Staff bdo" for bdo@desk.test. */
    private static String localPart(String email) {
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    /** A desk with the permissions its Keycloak composite carries. */
    private static RequestPostProcessor desk(String email, String desk) {
        List<String> roles = new ArrayList<>();
        roles.add(desk);
        Permissions.deskComposites().getOrDefault(desk, List.of()).stream()
                .filter(role -> role.startsWith("PERM_"))
                .forEach(roles::add);
        return staff(email, roles.toArray(String[]::new));
    }
}
