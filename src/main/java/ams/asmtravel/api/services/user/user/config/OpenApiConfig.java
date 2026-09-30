package ams.asmtravel.api.services.user.user.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.utils.SpringDocUtils;

import java.time.LocalTime;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI. The global {@code security} requirement makes Authorize really send the token.
 * To use it, paste an access token from the admin dashboard into <em>Authorize</em>.
 */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(
                title = "AMS Travel API",
                version = "v1",
                description = """
                        Travel catalogue, identity, saved items, images and administration.

                        Reading the published catalogue needs no token. Everything else takes a                         Keycloak bearer token: sign in to the admin dashboard, copy the access                         token and paste it into **Authorize** — it is kept across reloads.

                        The same endpoints are in the Postman collection under api/postman."""),
        security = @SecurityRequirement(name = "keycloakBearer"))
@SecurityScheme(
        name = "keycloakBearer",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {

    // Documents every LocalTime as a time with an example (14:00).
    static {
        SpringDocUtils.getConfig().replaceWithSchema(
                LocalTime.class,
                new StringSchema().format("partial-time").example("14:00"));
    }
}
