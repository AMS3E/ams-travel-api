# AMS Travel API

The Spring Boot API behind the AMS Travel website (port 3000) and the admin dashboard (port 3001). It runs on port **8080**, and every endpoint is under `/api/v1`.

**Stack:** Spring Boot 4 · Java 25 · PostgreSQL + Liquibase · Redis cache · MinIO for images · Keycloak for sign-in · MapStruct · Lombok · springdoc

## Run

The API needs PostgreSQL, Redis, MinIO and Keycloak. Start the whole stack from `../deployment`:

```bash
docker compose up -d --build
```

To run only the API from source (with the other services still in Docker):

```bash
./gradlew bootRun        # Windows: .\gradlew.bat bootRun
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Main settings are in `src/main/resources/application.yml`. Each one reads an environment variable and has a local default, such as `SPRING_DATASOURCE_URL`, `REDIS_HOST`, `MINIO_ENDPOINT`, `KEYCLOAK_SERVER_URL` and `CORS_ALLOWED_ORIGINS`.

## Structure

```text
ams.asmtravel.api
  ApiApplication.java
  services/<service>/<feature>/   one package per service, then one per feature
```

A service owns every table in its part of the domain, and there is no shared package. A service with more than one feature (for example `taxonomy/category`, `taxonomy/tag`) gives each feature its own layers:

```text
controller/  config/  domain/  dto/request/  dto/response/  mapper/
repository/  service/ (interfaces)  service/impl/  exception/
```

Services talk to each other only through `service` interfaces, DTOs and domain events. They never use another service's repositories. `ServiceBoundaryTests` enforces these rules.

| Service | Base URLs (`/api/v1/…`) |
| --- | --- |
| `activity` | `/popular`, `/recommendations`, `/stamps` |
| `admin` | `/admin` (dashboard counts, catalogue seed) |
| `audit` | `/admin/audit` |
| `authorization` | `/permissions`, `/roles`, `/user-roles` |
| `badge` | `/badges` |
| `corridor` | `/corridors`, `/corridor-destinations` |
| `destination` | `/destinations`, `/stays`, `/stay-rooms`, `/foods`, `/food-menu-items` |
| `favorite` | `/favorites` |
| `history` | `/destination-histories`, `/destination-monarchs`, `/historical-coverage/*` |
| `location` | `/provinces`, `/regions` |
| `media` | `/media`, `/media/files` |
| `notification` | `/notifications` |
| `point` | `/points` |
| `publishing` | `/publish-requests` |
| `review` | `/reviews`, `/ratings` |
| `season` | `/seasons` |
| `taxonomy` | `/categories`, `/interests`, `/tags` |
| `user` | `/users`, `/staff`, `/identities`, `/preferences` |
| `verification` | `/destination-verifications` |

Endpoints for the signed-in user's own data are under `/me` in the service that owns that data, for example `/users/me`, `/reviews/me` and `/points/me`.

## Security

- Tokens come from two Keycloak realms: `amstravel` for customers and `amsadmin` for staff.
- Staff roles are `PARTNER`, `CONTRIBUTOR`, `EDITOR`, `BDO` and `ADMIN`. Each role grants a set of `PERM_*` permissions.
- All URL access rules live in one file, `authorization/security/KeycloakSecurityConfig`. Most reads are public, anything under `/admin` needs a staff role, and any other request needs a signed-in user.
- Rules about which rows a user can see or change are enforced inside each service.

Keycloak lives with this project too:
- `src/main/resources/login` holds the CSS-only `amstravel` login theme. Compose mounts it into Keycloak.
- The `keycloak` stage of the `Dockerfile` builds the image with the staff email-code step, using the source in `src/main/keycloak`.

## Data

- **Schema:** Liquibase is the only thing that changes it, starting from `db/changelog/db.changelog-master.yaml`. Hibernate only validates it.
- **Seed data:** JSON files and photos live under `src/main/resources/ams/asmtravel/api/services/<service>/`. They load on first start when the catalogue is empty, and `SEED_CATALOG=false` turns this off.
- **Images:** all images are stored in MinIO. Rows keep the object key, and `MediaService.url(key)` turns it into a link.

## Tests

```bash
./gradlew test
```

Tests run on in-memory H2 in PostgreSQL mode and cover:
- the service boundaries and access rules
- the Liquibase changelog and the seed data
- each service's endpoints
