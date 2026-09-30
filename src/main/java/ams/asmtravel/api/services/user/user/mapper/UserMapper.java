package ams.asmtravel.api.services.user.user.mapper;

import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.user.user.domain.TokenIdentity;
import ams.asmtravel.api.services.user.user.domain.User;
import ams.asmtravel.api.services.user.user.domain.UserSummary;
import ams.asmtravel.api.services.user.user.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String REALMS_SEGMENT = "/realms/";

    @Autowired
    protected MediaService mediaService;

    /** @param roles the roles held, from the userrole service */
    @Mapping(target = "avatarUrl", expression = "java(mediaService.url(user.getAvatarKey()))")
    @Mapping(target = "roles", expression = "java(roleNames(roles))")
    public abstract UserResponse toResponse(User user, Set<RoleCode> roles);

    @Mapping(target = "roles", source = "roles")
    public abstract UserSummary toSummary(User user, Set<RoleCode> roles);

    /**
     * Reads a verified token: email trimmed and lower-cased, blank claims read as absent.
     * Roles come from the authorities Spring Security granted.
     *
     * @param customerRealm   the realm customers sign in to
     * @param backofficeRealm the realm staff sign in to
     */
    public TokenIdentity toIdentity(Jwt jwt, Collection<? extends GrantedAuthority> authorities,
                                    String customerRealm, String backofficeRealm) {
        Set<RoleCode> roles = roles(authorities);
        String provider = provider(jwt, roles, customerRealm, backofficeRealm);
        return new TokenIdentity(provider, jwt.getSubject(), email(jwt), name(jwt), roles,
                provider.equals(backofficeRealm));
    }

    /** Role codes, lowest first ({@code USER} … {@code ADMIN}). */
    protected List<String> roleNames(Set<RoleCode> roles) {
        Set<RoleCode> ordered = EnumSet.noneOf(RoleCode.class);
        if (roles != null) {
            ordered.addAll(roles);
        }
        return ordered.stream().map(Enum::name).toList();
    }

    private static Set<RoleCode> roles(Collection<? extends GrantedAuthority> authorities) {
        Set<RoleCode> roles = EnumSet.noneOf(RoleCode.class);
        for (GrantedAuthority authority : authorities) {
            String name = authority.getAuthority();
            if (name != null && name.startsWith(ROLE_PREFIX)) {
                RoleCode.parse(name.substring(ROLE_PREFIX.length())).ifPresent(roles::add);
            }
        }
        return roles;
    }

    /**
     * The realm from the issuer ({@code …/realms/amsadmin}).
     * Test tokens have no issuer and count as staff when they hold a back-office role.
     */
    private static String provider(Jwt jwt, Set<RoleCode> roles, String customerRealm, String backofficeRealm) {
        String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
        if (issuer != null && issuer.contains(REALMS_SEGMENT)) {
            return issuer.substring(issuer.lastIndexOf(REALMS_SEGMENT) + REALMS_SEGMENT.length())
                    .replaceAll("/+$", "");
        }
        return roles.stream().anyMatch(RoleCode::isBackoffice) ? backofficeRealm : customerRealm;
    }

    private static String email(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /** The realm's display name, falling back to the username when it has none. */
    private static String name(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name == null || name.isBlank()) {
            name = jwt.getClaimAsString("preferred_username");
        }
        return name == null || name.isBlank() ? null : name.trim();
    }
}
