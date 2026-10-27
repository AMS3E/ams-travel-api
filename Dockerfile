# The AMS Travel API, and the Keycloak image with the staff email-code step.
# Built from this folder by deploy/docker-compose.yml:
#   docker compose build api        (target runtime)
#   docker compose build keycloak   (target keycloak)

# ---- Keycloak: stock image plus the email-code step, as the ready-built jar the local Keycloak runs ----
FROM quay.io/keycloak/keycloak:26.3.1 AS keycloak
COPY --chown=1000:0 deploy/keycloak/providers/ams-email-code.jar /opt/keycloak/providers/ams-email-code.jar
# The AMS Travel look for Keycloak's pages, the two realm files (placeholders filled by the entrypoint
# from the environment) and the entrypoint itself. Built for Postgres; a laptop and a server run the same image.
COPY --chown=1000:0 src/main/resources/login /opt/keycloak/themes/amstravel/login
COPY --chown=1000:0 deploy/keycloak/realms /opt/keycloak/realm-templates
COPY --chown=1000:0 deploy/keycloak/entrypoint.sh /opt/keycloak/bin/ams-entrypoint.sh
RUN chmod 755 /opt/keycloak/bin/ams-entrypoint.sh && /opt/keycloak/bin/kc.sh build --db=postgres --health-enabled=true
ENTRYPOINT ["/opt/keycloak/bin/ams-entrypoint.sh"]
CMD ["start", "--optimized", "--import-realm"]

# ---- API ----
FROM eclipse-temurin:25-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src

# Gradle and the libraries are kept in a build cache, so a code change does not download them again.
RUN --mount=type=cache,target=/root/.gradle ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:25-jre-jammy AS runtime

WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring

COPY --from=build --chown=spring:spring /workspace/build/libs/*.jar /app/app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
