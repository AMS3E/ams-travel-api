package ams.asmtravel.api.services.authorization.security;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The two pieces of token plumbing {@link KeycloakSecurityConfig} wires into the
 * resource server: which issuers are trusted, and which claim becomes an
 * authority. {@code OAuth2AuthorizationRulesIntegrationTests} covers the rules
 * that consume those authorities.
 */
class KeycloakSecurityConfigTests {

    private static final String CUSTOMER_ISSUER = "http://localhost:9090/realms/amstravel";
    private static final String ADMIN_ISSUER = "http://localhost:9090/realms/amsadmin";

    @Nested
    class RealmRoles {

        @Test
        void realmRolesBecomePrefixedAuthorities() {
            var authorities = KeycloakSecurityConfig.realmRoles(jwtWithClaims(
                    Map.of("realm_access", Map.of("roles", List.of("ADMIN", "USER")))), null);

            assertThat(names(authorities)).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        }

        @Test
        void clientRolesAreIgnored() {
            var authorities = KeycloakSecurityConfig.realmRoles(jwtWithClaims(Map.of(
                    "realm_access", Map.of("roles", List.of("USER")),
                    "resource_access", Map.of("amsadmin-dashboard", Map.of("roles", List.of("ADMIN"))))),
                    null);

            assertThat(names(authorities)).containsExactly("ROLE_USER");
        }

        @Test
        void tokenWithoutRealmAccessGrantsNothing() {
            assertThat(KeycloakSecurityConfig.realmRoles(jwtWithClaims(Map.of()), null)).isEmpty();
        }

        @Test
        void malformedRealmAccessGrantsNothing() {
            assertThat(KeycloakSecurityConfig.realmRoles(
                    jwtWithClaims(Map.of("realm_access", Map.of("roles", "ADMIN"))), null)).isEmpty();
        }

        @Test
        void blankAndDuplicateRolesAreDropped() {
            var authorities = KeycloakSecurityConfig.realmRoles(jwtWithClaims(
                    Map.of("realm_access", Map.of("roles", List.of("ADMIN", " ", "ADMIN", " USER ")))), null);

            assertThat(names(authorities)).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
        }

        @Test
        void customerRealmCannotGrantBackofficeAuthorities() {
            Jwt customerToken = jwtWithClaims(Map.of(
                    "iss", CUSTOMER_ISSUER,
                    "realm_access", Map.of("roles", List.of("USER", "ADMIN", "PERM_FULL_CONTROL"))));

            assertThat(names(KeycloakSecurityConfig.realmRoles(customerToken, ADMIN_ISSUER)))
                    .containsExactly("ROLE_USER");
        }

        @Test
        void adminRealmCanGrantBackofficeAuthorities() {
            Jwt adminToken = jwtWithClaims(Map.of(
                    "iss", ADMIN_ISSUER,
                    "realm_access", Map.of("roles", List.of("ADMIN", "PERM_FULL_CONTROL"))));

            assertThat(names(KeycloakSecurityConfig.realmRoles(adminToken, ADMIN_ISSUER)))
                    .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_PERM_FULL_CONTROL");
        }
    }

    @Nested
    class TrustedIssuerDecoder {

        @Test
        void delegatesOnlyToTheDecoderRegisteredForTheTokenIssuer() {
            Jwt customer = decoded(CUSTOMER_ISSUER);
            Jwt admin = decoded(ADMIN_ISSUER);
            JwtDecoder decoder = KeycloakSecurityConfig.trustedIssuerDecoder(Map.of(
                    CUSTOMER_ISSUER, token -> customer,
                    ADMIN_ISSUER, token -> admin));

            assertThat(decoder.decode(token(CUSTOMER_ISSUER))).isSameAs(customer);
            assertThat(decoder.decode(token(ADMIN_ISSUER))).isSameAs(admin);
        }

        @Test
        void rejectsAnIssuerOutsideTheAllowListBeforeDelegating() {
            JwtDecoder decoder = KeycloakSecurityConfig.trustedIssuerDecoder(Map.of(
                    CUSTOMER_ISSUER, token -> decoded(CUSTOMER_ISSUER)));

            assertThatThrownBy(() -> decoder.decode(token("https://attacker.example/realm")))
                    .isInstanceOf(BadJwtException.class)
                    .hasMessageContaining("not trusted");
        }

        @Test
        void rejectsMalformedTokens() {
            JwtDecoder decoder = KeycloakSecurityConfig.trustedIssuerDecoder(Map.of(
                    CUSTOMER_ISSUER, token -> decoded(CUSTOMER_ISSUER)));

            assertThatThrownBy(() -> decoder.decode("not-a-jwt"))
                    .isInstanceOf(BadJwtException.class)
                    .hasMessageContaining("not a valid JWT");
        }

        @Test
        void refusesToTrustNothing() {
            assertThatThrownBy(() -> KeycloakSecurityConfig.trustedIssuerDecoder(Map.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    /* ---------------------------------------------------------------------- */

    private static List<String> names(Iterable<GrantedAuthority> authorities) {
        return java.util.stream.StreamSupport.stream(authorities.spliterator(), false)
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    private static Jwt jwtWithClaims(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("6f1b0f0e-0000-4000-8000-000000000000")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        claims.forEach(builder::claim);
        return builder.build();
    }

    private static Jwt decoded(String issuer) {
        return Jwt.withTokenValue("decoded")
                .header("alg", "RS256")
                .issuer(issuer)
                .subject("subject")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    private static String token(String issuer) {
        String header = encoded("{\"alg\":\"RS256\"}");
        String claims = encoded("{\"iss\":\"" + issuer + "\"}");
        return header + "." + claims + ".signature";
    }

    private static String encoded(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
