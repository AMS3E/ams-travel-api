package ams.asmtravel.api.services.authorization.security;

import ams.asmtravel.api.services.authorization.domain.Permissions;
import com.nimbusds.jwt.JWTParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfigurationSource;

import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * All API access rules and the Keycloak token-to-authority mapping.
 * First matching rule wins, so a narrow rule must sit above the broad one.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class KeycloakSecurityConfig {

    /** The desk roles a back-office token may carry. */
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_BDO = "BDO";
    private static final String ROLE_EDITOR = "EDITOR";
    private static final String ROLE_CONTRIBUTOR = "CONTRIBUTOR";
    private static final String ROLE_PARTNER = "PARTNER";

    /** The only role a customer ({@code amstravel}) token may carry; it just means "signed in". */
    private static final String VISITOR_ROLE = "USER";

    /** All desk roles. */
    private static final String[] DESKS = {ROLE_ADMIN, ROLE_BDO, ROLE_EDITOR, ROLE_CONTRIBUTOR, ROLE_PARTNER};

    private final CorsConfigurationSource corsConfigurationSource;

    @Value("${keycloak.auth-server-url:http://localhost:9090}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm:amstravel}")
    private String keycloakRealm;

    @Value("${keycloak.issuer-uri:}")
    private String keycloakIssuerUri;

    @Value("${keycloak.backoffice-realm:amsadmin}")
    private String keycloakBackofficeRealm;

    @Value("${keycloak.backoffice-issuer-uri:}")
    private String keycloakBackofficeIssuerUri;

    /** One decoder per realm, picked by the token's issuer; unknown issuers are refused. */
    @Bean
    @ConditionalOnProperty(name = "app.security.oauth2.enabled", havingValue = "true")
    public JwtDecoder jwtDecoder() {
        String serverUrl = keycloakServerUrl.replaceAll("/+$", "");
        String customerIssuer = issuer(serverUrl, keycloakRealm, keycloakIssuerUri);
        String backofficeIssuer = issuer(serverUrl, keycloakBackofficeRealm, keycloakBackofficeIssuerUri);

        Map<String, JwtDecoder> trustedIssuers = new LinkedHashMap<>();
        trustedIssuers.put(customerIssuer, realmDecoder(serverUrl, keycloakRealm, customerIssuer));
        trustedIssuers.put(backofficeIssuer, realmDecoder(serverUrl, keycloakBackofficeRealm, backofficeIssuer));
        return trustedIssuerDecoder(trustedIssuers);
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.oauth2.enabled", havingValue = "true")
    public SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource));
        http.authorizeHttpRequests(auth -> auth

                /* Swagger docs are public. */
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                /* Spring's error page must stay open, or an error turns into a second 401/403. */
                .requestMatchers("/error").permitAll()

                /* Admin dashboard overview: admin and editors. */
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/overview")
                        .hasAnyAuthority(Permissions.FULL_CONTROL, Permissions.REVIEW_MODERATE)
                /* Destination Stats: ratings, browses and the Verified standing of every place; the editorial desks. */
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/destination-stats")
                        .hasAnyAuthority(Permissions.FULL_CONTROL, Permissions.DESTINATION_EDIT_ANY)

                /* Amenities: what a stay offers, at the property and in the room. */
                .requestMatchers(HttpMethod.GET, "/api/v1/amenities", "/api/v1/amenities/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/amenities")
                        .hasAnyAuthority(Permissions.TAXONOMY_MANAGE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.PUT, "/api/v1/amenities/*")
                        .hasAnyAuthority(Permissions.TAXONOMY_MANAGE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/amenities/*").hasAuthority(Permissions.FULL_CONTROL)

                /* Audit: only admin may clear it. */
                .requestMatchers(HttpMethod.DELETE, "/api/v1/admin/audit").hasAuthority(Permissions.FULL_CONTROL)
                .requestMatchers("/api/v1/admin/audit")
                        .hasAnyAuthority(Permissions.AUDIT_VIEW, Permissions.FULL_CONTROL)

                /* Badges: definitions and share links are public; a person's own need a sign-in; admin writes. */
                .requestMatchers("/api/v1/badges/me/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/badges", "/api/v1/badges/*", "/api/v1/badges/shared/*",
                        "/api/v1/badges/shared/*/page").permitAll()
                .requestMatchers("/api/v1/badges", "/api/v1/badges/*").hasAuthority(Permissions.FULL_CONTROL)

                /* Categories. */
                .requestMatchers(HttpMethod.GET, "/api/v1/categories", "/api/v1/categories/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/categories")
                        .hasAnyAuthority(Permissions.TAXONOMY_MANAGE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.PUT, "/api/v1/categories/*")
                        .hasAnyAuthority(Permissions.TAXONOMY_MANAGE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/*").hasAuthority(Permissions.FULL_CONTROL)

                /* Corridors: public read; the service answers 404 (not 403) for drafts. */
                .requestMatchers(HttpMethod.GET, "/api/v1/corridors", "/api/v1/corridors/*",
                        "/api/v1/corridors/*/itinerary").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/corridors", "/api/v1/corridors/*/route",
                        "/api/v1/corridors/route-preview")
                        .hasAnyAuthority(Permissions.CORRIDOR_CURATE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.PUT, "/api/v1/corridors/*", "/api/v1/corridors/*/itinerary")
                        .hasAnyAuthority(Permissions.CORRIDOR_CURATE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/corridors/*/publish", "/api/v1/corridors/*/unpublish")
                        .hasAnyAuthority(Permissions.CORRIDOR_CURATE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/corridors/*").hasAuthority(Permissions.FULL_CONTROL)

                /* Corridor stops, saved as one list. */
                .requestMatchers(HttpMethod.GET, "/api/v1/corridor-destinations").permitAll()
                .requestMatchers(HttpMethod.PUT, "/api/v1/corridor-destinations/*")
                        .hasAnyAuthority(Permissions.CORRIDOR_CURATE, Permissions.FULL_CONTROL)

                /* Destinations: workspace must stay above the public reads; row checks are in DestinationPermissionEvaluator. */
                .requestMatchers(HttpMethod.GET, "/api/v1/destinations/workspace").hasAnyRole(DESKS)
                .requestMatchers(HttpMethod.GET, "/api/v1/destinations", "/api/v1/destinations/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/destinations")
                        .hasAnyAuthority(Permissions.LISTING_EDIT_OWN, Permissions.DESTINATION_DRAFT_CREATE,
                                Permissions.DESTINATION_EDIT_ANY, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.PUT, "/api/v1/destinations/*")
                        .hasAnyAuthority(Permissions.LISTING_EDIT_OWN, Permissions.DESTINATION_DRAFT_EDIT_OWN,
                                Permissions.DESTINATION_EDIT_ANY, Permissions.PARTNER_LISTING_EDIT,
                                Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/destinations/*")
                        .hasAnyAuthority(Permissions.DESTINATION_DELETE, Permissions.LISTING_DELETE_OWN,
                                Permissions.DESTINATION_DRAFT_DELETE_OWN, Permissions.PARTNER_LISTING_DELETE,
                                Permissions.FULL_CONTROL)
                /* Publish and unpublish are separate grants. */
                .requestMatchers(HttpMethod.PATCH, "/api/v1/destinations/*/publish")
                        .hasAnyAuthority(Permissions.DESTINATION_PUBLISH, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/destinations/*/unpublish")
                        .hasAnyAuthority(Permissions.DESTINATION_UNPUBLISH, Permissions.FULL_CONTROL)

                /* Favorites: the signed-in user's own. */
                .requestMatchers("/api/v1/favorites", "/api/v1/favorites/**").authenticated()

                /* Food listings: delete needs the delete grants, not the edit ones. */
                .requestMatchers(HttpMethod.GET, "/api/v1/foods", "/api/v1/foods/*").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/foods/*")
                        .hasAnyAuthority(Permissions.LISTING_DELETE_OWN, Permissions.DESTINATION_DELETE,
                                Permissions.DESTINATION_EDIT_ANY, Permissions.FULL_CONTROL)
                .requestMatchers("/api/v1/foods", "/api/v1/foods/*")
                        .hasAnyAuthority(Permissions.LISTING_EDIT_OWN, Permissions.DESTINATION_EDIT_ANY,
                                Permissions.FULL_CONTROL)

                /* Food menu items: same editors as the listing. */
                .requestMatchers(HttpMethod.GET, "/api/v1/food-menu-items", "/api/v1/food-menu-items/*").permitAll()
                .requestMatchers("/api/v1/food-menu-items", "/api/v1/food-menu-items/*")
                        .hasAnyAuthority(Permissions.LISTING_EDIT_OWN, Permissions.DESTINATION_EDIT_ANY,
                                Permissions.FULL_CONTROL)

                /* Identities: user management only. */
                .requestMatchers("/api/v1/identities", "/api/v1/identities/**")
                        .hasAnyAuthority(Permissions.USER_MANAGE, Permissions.FULL_CONTROL)

                /* Interests. */
                .requestMatchers(HttpMethod.GET, "/api/v1/interests", "/api/v1/interests/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/interests")
                        .hasAnyAuthority(Permissions.TAXONOMY_MANAGE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.PUT, "/api/v1/interests/*")
                        .hasAnyAuthority(Permissions.TAXONOMY_MANAGE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/interests/*").hasAuthority(Permissions.FULL_CONTROL)

                /* Media files first (the private verification/ folder before the public read), then galleries. */
                .requestMatchers(HttpMethod.GET, "/api/v1/media/files/verification/*/*")
                        .hasAnyAuthority(Permissions.DESTINATION_VERIFY, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.GET, "/api/v1/media/files/*/*/*").permitAll()
                .requestMatchers("/api/v1/media/files", "/api/v1/media/files/**").authenticated()
                /* Galleries (destination, stay, food): the service checks the owner's editors. */
                .requestMatchers(HttpMethod.GET, "/api/v1/media", "/api/v1/media/*").permitAll()
                .requestMatchers("/api/v1/media", "/api/v1/media/*")
                        .hasAnyAuthority(Permissions.LISTING_EDIT_OWN, Permissions.DESTINATION_DRAFT_EDIT_OWN,
                                Permissions.DESTINATION_EDIT_ANY, Permissions.PARTNER_LISTING_EDIT,
                                Permissions.FULL_CONTROL)

                /* Notifications: the signed-in user's own. */
                .requestMatchers("/api/v1/notifications", "/api/v1/notifications/**").authenticated()

                /* Permissions list: whoever assigns roles. */
                .requestMatchers("/api/v1/permissions", "/api/v1/permissions/**")
                        .hasAnyAuthority(Permissions.ROLE_ASSIGN, Permissions.FULL_CONTROL)

                /* Popular: browse events come from anonymous visitors too. */
                .requestMatchers(HttpMethod.POST, "/api/v1/popular/events").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/popular").permitAll()

                /* Preferences: the signed-in user's own. */
                .requestMatchers("/api/v1/preferences", "/api/v1/preferences/**").authenticated()

                /* Provinces. */
                .requestMatchers(HttpMethod.GET, "/api/v1/provinces", "/api/v1/provinces/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/provinces")
                        .hasAnyAuthority(Permissions.TAXONOMY_MANAGE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.PUT, "/api/v1/provinces/*")
                        .hasAnyAuthority(Permissions.TAXONOMY_MANAGE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/provinces/*").hasAuthority(Permissions.FULL_CONTROL)

                /* Publish requests: writers submit; the publishing service checks which desk may decide. */
                .requestMatchers(HttpMethod.POST, "/api/v1/publish-requests")
                        .hasAnyAuthority(Permissions.PUBLISH_REQUEST_SUBMIT, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.POST, "/api/v1/publish-requests/*/approve",
                        "/api/v1/publish-requests/*/reject", "/api/v1/publish-requests/*/publish")
                        .hasAnyAuthority(Permissions.PARTNER_PUBLISH_APPROVE, Permissions.CONTRIBUTOR_PUBLISH_APPROVE,
                                Permissions.DESTINATION_PUBLISH, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.POST, "/api/v1/publish-requests/*/unpublish")
                        .hasAnyAuthority(Permissions.DESTINATION_UNPUBLISH, Permissions.FULL_CONTROL)
                .requestMatchers("/api/v1/publish-requests", "/api/v1/publish-requests/**")
                        .hasAnyAuthority(Permissions.PUBLISH_REQUEST_VIEW_OWN, Permissions.PARTNER_PUBLISH_APPROVE,
                                Permissions.CONTRIBUTOR_PUBLISH_APPROVE, Permissions.FULL_CONTROL)

                /* Points: a person's own. */
                .requestMatchers("/api/v1/points", "/api/v1/points/**").authenticated()

                /* Ratings: averages and single ratings are public; your own and every write need a sign-in. */
                .requestMatchers(HttpMethod.GET, "/api/v1/ratings/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/ratings", "/api/v1/ratings/summary", "/api/v1/ratings/*").permitAll()
                .requestMatchers("/api/v1/ratings", "/api/v1/ratings/*").authenticated()

                /* Recommended: personal when signed in, seasonal otherwise. */
                .requestMatchers(HttpMethod.GET, "/api/v1/recommendations/for-you").permitAll()

                /* Regions (zones and tourism regions). */
                .requestMatchers(HttpMethod.GET, "/api/v1/regions", "/api/v1/regions/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/regions")
                        .hasAnyAuthority(Permissions.TAXONOMY_MANAGE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.PUT, "/api/v1/regions/*")
                        .hasAnyAuthority(Permissions.TAXONOMY_MANAGE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/regions/*").hasAuthority(Permissions.FULL_CONTROL)


                /* Reviews: published ones are public; the service shows others only to their author or a moderator. */
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews", "/api/v1/reviews/summary", "/api/v1/reviews/*").permitAll()
                .requestMatchers("/api/v1/reviews", "/api/v1/reviews/*").authenticated()

                /* Roles. */
                .requestMatchers("/api/v1/roles", "/api/v1/roles/**")
                        .hasAnyAuthority(Permissions.ROLE_ASSIGN, Permissions.FULL_CONTROL)

                /* Search: what the app's search box, filters, lists and cards read; published places only. */
                .requestMatchers(HttpMethod.GET, "/api/v1/search", "/api/v1/search/**").permitAll()

                /* Seasons: public read, admin write; a person's own totals need a sign-in. */
                .requestMatchers(HttpMethod.GET, "/api/v1/seasons/me/*").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/seasons", "/api/v1/seasons/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/seasons").hasAuthority(Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.PUT, "/api/v1/seasons/*").hasAuthority(Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/seasons/*").hasAuthority(Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.POST, "/api/v1/seasons/*/activate", "/api/v1/seasons/*/complete")
                        .hasAuthority(Permissions.FULL_CONTROL)

                /* Staff: creating and changing roles needs ROLE_ASSIGN. */
                .requestMatchers(HttpMethod.POST, "/api/v1/staff")
                        .hasAnyAuthority(Permissions.ROLE_ASSIGN, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.PUT, "/api/v1/staff/*/roles")
                        .hasAnyAuthority(Permissions.ROLE_ASSIGN, Permissions.FULL_CONTROL)
                .requestMatchers("/api/v1/staff", "/api/v1/staff/**")
                        .hasAnyAuthority(Permissions.USER_MANAGE, Permissions.FULL_CONTROL)

                /* Stamps (check-ins): signed-in users. */
                .requestMatchers("/api/v1/stamps", "/api/v1/stamps/**").authenticated()

                /* Stays. */
                .requestMatchers(HttpMethod.GET, "/api/v1/stays", "/api/v1/stays/*").permitAll()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/stays/*")
                        .hasAnyAuthority(Permissions.LISTING_DELETE_OWN, Permissions.DESTINATION_DELETE,
                                Permissions.DESTINATION_EDIT_ANY, Permissions.FULL_CONTROL)
                .requestMatchers("/api/v1/stays", "/api/v1/stays/*")
                        .hasAnyAuthority(Permissions.LISTING_EDIT_OWN, Permissions.DESTINATION_EDIT_ANY,
                                Permissions.FULL_CONTROL)

                /* Stay rooms: same editors as the stay; the price preview is public. */
                .requestMatchers(HttpMethod.GET, "/api/v1/stay-rooms/quote", "/api/v1/stay-rooms/*/quote").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/stay-rooms", "/api/v1/stay-rooms/*").permitAll()
                .requestMatchers("/api/v1/stay-rooms", "/api/v1/stay-rooms/*")
                        .hasAnyAuthority(Permissions.LISTING_EDIT_OWN, Permissions.DESTINATION_EDIT_ANY,
                                Permissions.FULL_CONTROL)

                /* Tags. */
                .requestMatchers(HttpMethod.GET, "/api/v1/tags", "/api/v1/tags/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/tags")
                        .hasAnyAuthority(Permissions.TAXONOMY_MANAGE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.PUT, "/api/v1/tags/*")
                        .hasAnyAuthority(Permissions.TAXONOMY_MANAGE, Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/tags/*").hasAuthority(Permissions.FULL_CONTROL)

                /* Users: /me must come first; authors feeds the byline picker. */
                .requestMatchers("/api/v1/users/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/users/authors")
                        .hasAnyAuthority(Permissions.DESTINATION_EDIT_ANY, Permissions.PARTNER_PUBLISH_APPROVE,
                                Permissions.CONTRIBUTOR_PUBLISH_APPROVE, Permissions.USER_MANAGE,
                                Permissions.FULL_CONTROL)
                .requestMatchers("/api/v1/users", "/api/v1/users/**")
                        .hasAnyAuthority(Permissions.USER_MANAGE, Permissions.FULL_CONTROL)

                /* User roles. */
                .requestMatchers("/api/v1/user-roles", "/api/v1/user-roles/**")
                        .hasAnyAuthority(Permissions.USER_MANAGE, Permissions.ROLE_ASSIGN, Permissions.FULL_CONTROL)

                /* Verification: status is public; the rest is BDO and admin only. */
                .requestMatchers(HttpMethod.GET, "/api/v1/destination-verifications/status").permitAll()
                .requestMatchers("/api/v1/destination-verifications", "/api/v1/destination-verifications/queue")
                        .hasAnyAuthority(Permissions.DESTINATION_VERIFY, Permissions.FULL_CONTROL)

                /* Any other /admin path: any desk. */
                .requestMatchers("/api/v1/admin/**").hasAnyRole(DESKS)

                /* Fallback: public read, admin write. ServiceBoundaryTests fails until a new base URL has its own rules. */
                .requestMatchers(HttpMethod.GET, "/api/v1/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/**").hasAuthority(Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.PUT, "/api/v1/**").hasAuthority(Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/**").hasAuthority(Permissions.FULL_CONTROL)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasAuthority(Permissions.FULL_CONTROL)

                /* CORS pre-flight carries no token. */
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
        );

        http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                .decoder(jwtDecoder())
                .jwtAuthenticationConverter(jwtAuthenticationConverter())));

        /* Bearer-token API: no login form, no CSRF, no session. */
        http.formLogin(AbstractHttpConfigurer::disable);
        http.csrf(AbstractHttpConfigurer::disable);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    /** Keycloak realm roles as {@code ROLE_} authorities; client roles are ignored on purpose. */
    @Bean
    @ConditionalOnProperty(name = "app.security.oauth2.enabled", havingValue = "true")
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        String serverUrl = keycloakServerUrl.replaceAll("/+$", "");
        String backofficeIssuer = issuer(serverUrl, keycloakBackofficeRealm, keycloakBackofficeIssuerUri);

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> realmRoles(jwt, backofficeIssuer));
        return converter;
    }

    /** With OAuth2 off everything is open (tests and local runs). */
    @Bean
    @ConditionalOnProperty(name = "app.security.oauth2.enabled", havingValue = "false", matchIfMissing = true)
    public SecurityFilterChain localDevelopmentSecurity(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource));
        http.csrf(AbstractHttpConfigurer::disable);
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /** Decoder for one realm: its keys and its exact issuer. */
    private JwtDecoder realmDecoder(String serverUrl, String realm, String issuer) {
        String jwkSetUri = serverUrl + "/realms/" + realm + "/protocol/openid-connect/certs";
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    /** Expected {@code iss}; configurable when it differs from the server URL. */
    private String issuer(String serverUrl, String realm, String configured) {
        return StringUtils.hasText(configured)
                ? configured.replaceAll("/+$", "")
                : serverUrl + "/realms/" + realm;
    }

    /** Routes a token to its issuer's decoder; unknown issuers are rejected. */
    static JwtDecoder trustedIssuerDecoder(Map<String, JwtDecoder> decoders) {
        if (decoders.isEmpty()) {
            throw new IllegalArgumentException("At least one trusted issuer is required");
        }
        Map<String, JwtDecoder> trusted = Map.copyOf(decoders);
        return token -> {
            String issuer;
            try {
                issuer = JWTParser.parse(token).getJWTClaimsSet().getIssuer();
            } catch (ParseException exception) {
                throw new BadJwtException("The bearer token is not a valid JWT", exception);
            }
            JwtDecoder decoder = issuer == null ? null : trusted.get(issuer);
            if (decoder == null) {
                throw new BadJwtException("The bearer token issuer is not trusted");
            }
            return decoder.decode(token);
        };
    }

    /** Only the back-office realm may grant desk roles; a customer token keeps just {@code USER}. */
    static Collection<GrantedAuthority> realmRoles(Jwt jwt, String backofficeIssuer) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return List.of();
        }
        Object claimed = realmAccess.get("roles");
        if (!(claimed instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .filter(role -> mayGrant(jwt, role, backofficeIssuer))
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)))
                .distinct()
                .toList();
    }

    private static boolean mayGrant(Jwt jwt, String role, String backofficeIssuer) {
        if (backofficeIssuer == null) {
            return true;
        }
        boolean fromBackoffice = jwt.getIssuer() != null && backofficeIssuer.equals(jwt.getIssuer().toString());
        return fromBackoffice || VISITOR_ROLE.equals(role);
    }
}
