package ams.asmtravel.api.services.authorization.domain;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Helpers to check the current caller's {@code ROLE_} authorities. */
public final class Authorities {

    private static final Set<String> BACKOFFICE = Arrays.stream(Permissions.backofficeRoles())
            .map(role -> "ROLE_" + role)
            .collect(Collectors.toUnmodifiableSet());

    private Authorities() {
    }

    public static Authentication current() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    public static boolean has(Authentication authentication, String authority) {
        return authentication != null && authentication.isAuthenticated() && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    public static boolean hasAny(Authentication authentication, String... authorities) {
        return Arrays.stream(authorities).anyMatch(authority -> has(authentication, authority));
    }

    /** True for any desk role (back-office users can see drafts). */
    public static boolean isBackoffice(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(BACKOFFICE::contains);
    }
}
