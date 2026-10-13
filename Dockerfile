# The AMS Travel API, and the Keycloak image with the staff email-code step.
# Built from this folder by deploy/docker-compose.yml:
#   docker compose build api        (target runtime)
#   docker compose build keycloak   (target keycloak)

# ---- Keycloak: stock image plus the email-code step (Java only, no templates) ----
FROM quay.io/keycloak/keycloak:26.3.1 AS keycloak-lib

FROM eclipse-temurin:21-jdk-jammy AS keycloak-provider
WORKDIR /provider
COPY --from=keycloak-lib /opt/keycloak/lib/lib /keycloak-lib
COPY src/main/keycloak ./src
# The provider's registration line is written into the jar here, not kept in the source.
RUN javac -g --release 21 -cp "/keycloak-lib/main/*:/keycloak-lib/boot/*" -d out $(find src -name "*.java") \
    && mkdir -p out/META-INF/services \
    && echo "ams.asmtravel.keycloak.emailcode.EmailCodeAuthenticatorFactory" > out/META-INF/services/org.keycloak.authentication.AuthenticatorFactory \
    && jar --create --file ams-email-code.jar -C out .

FROM quay.io/keycloak/keycloak:26.3.1 AS keycloak
COPY --from=keycloak-provider /provider/ams-email-code.jar /opt/keycloak/providers/ams-email-code.jar
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

RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:25-jre-jammy AS runtime

WORKDIR /app

RUN groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring

COPY --from=build --chown=spring:spring /workspace/build/libs/*.jar /app/app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
