package ams.asmtravel.api.services.authorization.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ams.asmtravel.api.services.authorization.domain.Realms;

/**
 * Keycloak admin clients, one per realm (client credentials; secrets come from the environment).
 * Put {@code @Qualifier} on constructor parameters, not fields: Lombok drops it from fields.
 */
@Configuration
public class KeycloakAdminConfig {

    /** The API's own client in each realm. */
    private static final String ADMIN = "amsadmin-api";
    private static final String CLIENT = "amstravel-api";

    /** Same Keycloak URL the token decoder uses. */
    @Value("${keycloak.auth-server-url:http://localhost:9090}")
    private String serverUrl;

    /** Client secrets, one per realm. */
    @Value("${keycloak.backoffice-client-secret:}")
    private String backofficeClientSecret;

    @Value("${keycloak.customer-client-secret:}")
    private String customerClientSecret;

    /** Back-office realm client; when admin is disabled the people endpoints answer 503. */
    @Bean(name = "amsadminKeycloak", destroyMethod = "close")
    @Primary
    @ConditionalOnProperty(prefix = "app.keycloak.admin", name = "enabled", havingValue = "true")
    public Keycloak amsadminKeycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(url())
                .realm(Realms.BACKOFFICE)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(ADMIN)
                .clientSecret(backofficeClientSecret)
                .build();
    }

    /** Customer realm client. */
    @Bean(name = "amstravelKeycloak", destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.keycloak.admin", name = "enabled", havingValue = "true")
    public Keycloak amstravelKeycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(url())
                .realm(Realms.CUSTOMER)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(CLIENT)
                .clientSecret(customerClientSecret)
                .build();
    }

    /** Strips the trailing slash to avoid double slashes in admin URLs. */
    private String url() {
        return serverUrl.replaceAll("/+$", "");
    }
}
