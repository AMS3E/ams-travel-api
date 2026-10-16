package ams.asmtravel.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The layout the API is kept in, checked against its source.
 *
 * <pre>
 * ams.asmtravel.api
 *   ApiApplication
 *   services/   one package per feature, owning its endpoints, data, config and rules
 * </pre>
 *
 * There is no shared package: what every service needs lives in the service it
 * belongs to — every rule about who may call what in {@code authorization},
 * who is calling in {@code user}, links to stored files in {@code media}, the
 * catalogue seed in {@code admin} — and is reached, like anything else, through
 * that service's contract.
 *
 * Every service has the same layers, and only these:
 *
 * <pre>
 *   client         calls out to an external API — Google Routes — and nothing more
 *   controller     REST endpoints
 *   config         wiring and startup: seeders, initializers, registrations
 *   domain         entities, enums, and the events the service publishes
 *   dto/request    what a caller sends
 *   dto/response   what a caller gets back
 *   mapper         MapStruct mappers
 *   repository     data access
 *   security       permission beans
 *   service        interfaces — the service's contract
 *   service/impl   implementations, including listeners to other services' events
 * </pre>
 *
 * A service is one aggregate: a root table and the tables that exist only for
 * it — an account with its sign-ins, preferences and roles; a review with its
 * photos; a badge with its periods, progress and what people earned — or a set
 * of small tables read only together and written by one permission, as
 * {@code history} holds the eras, kings, building styles and faiths of Khmer
 * history. Each of those tables has its own entity and its own repository, and each resource its
 * own controller under a base URL no other service answers on. A table's plain
 * join tables are {@code @ManyToMany} on the entity that owns them, not tables
 * of their own. A few services have no table: they read Keycloak, MinIO or
 * other services' data.
 *
 * A service is one level deep: its layers,
 * and nothing that is a service of its own. It reaches another only through
 * that service's contract — its service interfaces, DTOs and domain (entities
 * and events) — and never through its repositories, mappers, implementations,
 * controllers, config or permission beans. Entities belong to the contract
 * because the services share one database and map its foreign keys as JPA
 * relations.
 */
class ServiceBoundaryTests {

    private static final Path SOURCES = Path.of("src/main/java/ams/asmtravel/api");
    private static final Path RESOURCES = Path.of("src/main/resources/ams/asmtravel/api");
    private static final Set<String> LAYERS = Set.of("client", "controller", "config", "domain", "dto",
            "exception", "mapper", "repository", "security", "service", "specification");
    /** Every package a service may put a class in, as a path under the service. */
    private static final Set<String> PACKAGES = Set.of("client", "controller", "config", "domain", "dto/request",
            "dto/response", "exception", "mapper", "repository", "security", "service", "service/impl", "specification");
    /**
     * The layers every feature must fill.
     *
     * <p>Three are missing on purpose. {@code exception} is a feature's own to
     * have or not — a read-only list raises nothing, and a failure nothing
     * raises is worse than no class at all. {@code config} belongs to the
     * service rather than the feature: the docs group sits there, and a feature
     * with nothing to wire would otherwise need a class to fill the folder.
     * {@code security} is {@code authorization}'s: every service's access rules
     * are gathered there, and only the four per-row evaluators still keep one.
     * That a feature's endpoints are covered is checked from the other end, in
     * {@link #everyEndpointHasARuleInAuthorization()}.</p>
     */
    private static final Set<String> REQUIRED = Set.of("controller", "domain", "dto/request",
            "dto/response", "mapper", "repository", "service", "service/impl");
    /**
     * What one service may use of another: its contract. Exceptions are part of
     * it, so the service that owns a row is the one that says it is missing —
     * "Destination not found" is written once, not at every place that looks
     * one up.
     */
    private static final Set<String> CONTRACT =
            Set.of("domain", "dto", "request", "response", "service", "exception");
    /** Features with no table of their own: they read Keycloak, MinIO, or other services' rows. */
    private static final Set<String> TABLELESS = Set.of("realm", "recommendation", "search");
    private static final Pattern ENTITY = Pattern.compile("^@Entity\\b", Pattern.MULTILINE);
    private static final Pattern BASE_URL = Pattern.compile("^@RequestMapping\\(\"([^\"]+)\"\\)", Pattern.MULTILINE);
    /** The entity a repository is for, as {@code JpaRepository<Review, …>} or a query over it. */
    private static final java.util.function.Function<String, Pattern> ENTITY_IN_REPOSITORY =
            entity -> Pattern.compile("\\b" + Pattern.quote(entity) + "\\b");

    private static final Pattern SERVICE_REFERENCE = Pattern.compile(
            "ams\\.asmtravel\\.api\\.services\\.([a-z0-9_]+)((?:\\.[a-z0-9_]+)*)\\.(?:[A-Z]\\w*|\\*)");

    @Test
    void theApiIsTheApplicationAndItsServices() throws IOException {
        try (Stream<Path> entries = Files.list(SOURCES)) {
            assertThat(entries.map(entry -> entry.getFileName().toString()))
                    .containsExactlyInAnyOrder("ApiApplication.java", "services");
        }
    }

    /**
     * A service is two levels deep at most: its features, and their layers.
     *
     * <p>{@code taxonomy/category/domain} is the shape; {@code taxonomy/category/thing/domain}
     * is not. A feature is where the layers live, and nothing lives below them.</p>
     */
    @Test
    void noFeatureIsNestedInsideAnother() throws IOException {
        List<String> nested = new ArrayList<>();
        for (Path feature : features()) {
            try (Stream<Path> folders = Files.list(feature)) {
                folders.filter(Files::isDirectory)
                        .filter(folder -> !LAYERS.contains(folder.getFileName().toString()))
                        .forEach(folder -> nested.add(SOURCES.relativize(folder).toString()));
            }
        }
        assertThat(nested).as("packages inside a feature that are not one of its layers").isEmpty();
    }

    /**
     * The agreed layout, checked class by class: a service's classes sit in
     * one of {@link #PACKAGES} and nowhere else — not in a layer of the
     * service's own invention, not loose in the service's root, and not in a
     * sub-package of a layer (a {@code dto} holds {@code request} and
     * {@code response}, a {@code service} holds {@code impl}). Nothing at all
     * sits in a service's root.
     */
    @Test
    void everyServiceClassSitsInAnAgreedLayer() throws IOException {
        Path services = SOURCES.resolve("services");
        List<String> misplaced = new ArrayList<>();
        for (Path file : javaFiles(services)) {
            Path inService = services.relativize(file);
            /* Either <service>/<layer>/X or <service>/<feature>/<layer>/X. */
            String layer = inService.getNameCount() > 2
                    ? inService.subpath(1, inService.getNameCount() - 1).toString().replace('\\', '/')
                    : "";
            String withinFeature = inService.getNameCount() > 3
                    ? inService.subpath(2, inService.getNameCount() - 1).toString().replace('\\', '/')
                    : "";
            if (!PACKAGES.contains(layer) && !PACKAGES.contains(withinFeature)) {
                misplaced.add(SOURCES.relativize(file).toString());
            }
        }
        assertThat(misplaced).as("classes outside controller, config, domain, dto/request, dto/response, "
                + "mapper, repository, security, service and service/impl").isEmpty();
    }

    /**
     * Every service has every layer, each holding code of its own — its own
     * entities, data access, requests and responses, mappers, endpoints,
     * settings, access rules, contract and implementation — so each one reads
     * the same way. A service with nothing for a layer needs a reason to have
     * something, or does not belong as a service. A folder with no Java in it at
     * all is not a service yet. {@code exception} is the one layer a service may leave empty,
     * because not every service raises a failure of its own.
     */
    @Test
    void everyServiceHasCodeInEveryLayer() throws IOException {
        List<String> missing = new ArrayList<>();
        for (Path service : features()) {
            for (String layer : REQUIRED.stream().sorted().toList()) {
                Path folder = service.resolve(layer);
                boolean present;
                try (Stream<Path> files = Files.isDirectory(folder) ? Files.list(folder) : Stream.empty()) {
                    present = files.map(file -> file.getFileName().toString())
                            .anyMatch(name -> name.endsWith(".java"));
                }
                if (!present) {
                    missing.add(SOURCES.relativize(folder).toString());
                }
            }
        }
        assertThat(missing).as("layers without code of their own").isEmpty();
    }

    /**
     * One table, one entity, and data access of its own.
     *
     * <p>Every {@code @Entity} in a service is named by a repository in the same
     * service, so no table is reachable only through another table's queries.
     * The reverse does not hold, and deliberately: a service may also keep a
     * repository over something that is not a table of its own — Keycloak's
     * staff realm, or a view of its own rows such as a rating counted from
     * reviews — so the two are not counted against each other. A service with
     * no table at all is one of {@link #TABLELESS}.</p>
     */
    @Test
    void everyTableInAServiceHasItsOwnEntityAndDataAccess() throws IOException {
        List<String> wrong = new ArrayList<>();
        for (Path service : features()) {
            String name = service.getFileName().toString();
            List<String> entities = new ArrayList<>();
            for (Path file : classFiles(service.resolve("domain"))) {
                if (ENTITY.matcher(Files.readString(file)).find()) {
                    entities.add(file.getFileName().toString().replace(".java", ""));
                }
            }
            if (TABLELESS.contains(name)) {
                if (!entities.isEmpty()) {
                    wrong.add(name + " has no table, but holds entities " + entities);
                }
                continue;
            }
            if (entities.isEmpty()) {
                wrong.add(name + " has no entity; add it to TABLELESS if it owns no table");
            }
            StringBuilder access = new StringBuilder();
            for (Path file : classFiles(service.resolve("repository"))) {
                access.append(Files.readString(file));
            }
            for (String entity : entities) {
                if (!ENTITY_IN_REPOSITORY.apply(entity).matcher(access).find()) {
                    wrong.add(name + "/domain/" + entity + " has no repository of its own");
                }
            }
        }
        assertThat(wrong).as("tables without data access of their own").isEmpty();
    }

    /** Every contract is fulfilled once: a service interface has one implementation, and no impl is orphaned. */
    @Test
    void everyServiceInterfaceHasOneImplementation() throws IOException {
        List<String> wrong = new ArrayList<>();
        for (Path service : features()) {
            List<String> contracts = classesIn(service.resolve("service"));
            List<String> impls = classesIn(service.resolve("service/impl"));
            for (String contract : contracts) {
                String expected = contract.replace(".java", "Impl.java");
                if (!impls.contains(expected)) {
                    wrong.add(service.getFileName() + "/service/" + contract + " has no " + expected);
                }
            }
            for (String impl : impls) {
                String expected = impl.replace("Impl.java", ".java");
                if (!contracts.contains(expected)) {
                    wrong.add(service.getFileName() + "/service/impl/" + impl + " implements no contract of its own");
                }
            }
        }
        assertThat(wrong).as("service contracts without exactly one implementation").isEmpty();
    }

    /**
     * A service's endpoints hang off one literal base URL of its own, so no two
     * services answer under the same path and a URL says which service owns it.
     */
    @Test
    void everyServiceHasABaseUrlOfItsOwn() throws IOException {
        List<String> wrong = new ArrayList<>();
        Map<String, String> owners = new HashMap<>();
        for (Path service : features()) {
            String name = service.getFileName().toString();
            for (Path controller : classFiles(service.resolve("controller"))) {
                Matcher base = BASE_URL.matcher(Files.readString(controller));
                List<String> urls = new ArrayList<>();
                while (base.find()) {
                    urls.add(base.group(1));
                }
                if (urls.size() != 1 || !urls.getFirst().startsWith("/api/v1/")) {
                    wrong.add(name + ": " + controller.getFileName() + " maps " + urls);
                    continue;
                }
                String previous = owners.putIfAbsent(urls.getFirst(), name);
                if (previous != null) {
                    wrong.add(urls.getFirst() + " belongs to both " + previous + " and " + name);
                }
            }
        }
        assertThat(wrong).as("controllers without one base URL of their own").isEmpty();
    }

    /**
     * Who may call what is said in one place: {@code authorization/security}.
     *
     * <p>Every base URL in the API has to be named by a rule there. An endpoint
     * nobody wrote a rule for is not closed — it falls through to the API-wide
     * defaults, which make GETs public — so an unnamed base URL is a silent
     * hole rather than a missing file.</p>
     */
    @Test
    void everyEndpointHasARuleInAuthorization() throws IOException {
        Path rules = SOURCES.resolve("services/authorization/security");
        assertThat(Files.isDirectory(rules)).as("authorization/security holds the access rules").isTrue();
        String declared = String.join("\n", javaFiles(rules).stream().map(file -> {
            try {
                return Files.readString(file);
            } catch (IOException problem) {
                throw new java.io.UncheckedIOException(problem);
            }
        }).toList());

        /* Every path a rule names, whether the base itself or something under it. */
        List<String> guarded = Pattern.compile("\"(/api/v1/[^\"]*)\"").matcher(declared).results()
                .map(match -> match.group(1))
                .toList();

        List<String> uncovered = new ArrayList<>();
        for (Path feature : features()) {
            for (Path controller : classFiles(feature.resolve("controller"))) {
                Matcher base = BASE_URL.matcher(Files.readString(controller));
                while (base.find()) {
                    String url = base.group(1);
                    /* A rule may name the base, or only the paths under it — `/admin/overview` is all `/admin` has. */
                    if (guarded.stream().noneMatch(path -> path.equals(url) || path.startsWith(url + "/"))) {
                        uncovered.add(url + " (" + controller.getFileName() + ")");
                    }
                }
            }
        }
        assertThat(uncovered).as("base URLs no access rule in authorization names").isEmpty();
    }

    /**
     * The rules are all in one place, and nowhere else keeps one.
     *
     * <p>They are the sections of {@code KeycloakSecurityConfig.apiSecurity}
     * since 2026-09-25. A service that declared a {@code requestMatchers} of
     * its own would be a second answer to "who may call this?", found only by
     * whoever happened to open that file.</p>
     */
    @Test
    void noServiceKeepsAccessRulesOfItsOwn() throws IOException {
        List<String> strays = new ArrayList<>();
        for (Path file : javaFiles(SOURCES.resolve("services"))) {
            boolean declaresRules = Files.readString(file).contains("requestMatchers(");
            boolean inAuthorization = file.getParent().endsWith(Path.of("authorization", "security"));
            if (declaresRules && !inAuthorization) {
                strays.add(SOURCES.relativize(file).toString());
            }
        }
        assertThat(strays).as("access rules outside authorization/security").isEmpty();
    }

    /**
     * {@code service} is the contract and {@code service/impl} what fulfils it:
     * a class in {@code service} is an implementation another service could
     * reach past its interface, and a value type there belongs in
     * {@code domain} or {@code dto}.
     */
    @Test
    void serviceHoldsInterfacesAndImplHoldsTheirImplementations() throws IOException {
        Path services = SOURCES.resolve("services");
        Pattern declaration = Pattern.compile("^public\\s+(?:final\\s+|abstract\\s+)*(interface|class|record|enum)\\b",
                Pattern.MULTILINE);
        List<String> misplaced = new ArrayList<>();
        for (Path file : javaFiles(services)) {
            Path inService = services.relativize(file);
            if (inService.getNameCount() < 3) {
                continue;
            }
            String layer = inService.subpath(inService.getNameCount() - 2, inService.getNameCount() - 1)
                    .toString().replace('\\', '/');
            if (inService.getNameCount() > 2
                    && inService.subpath(inService.getNameCount() - 2, inService.getNameCount() - 1)
                            .toString().equals("impl")) {
                layer = "service/impl";
            }
            if (!layer.equals("service") && !layer.equals("service/impl")) {
                continue;
            }
            Matcher kind = declaration.matcher(Files.readString(file));
            boolean isInterface = kind.find() && kind.group(1).equals("interface");
            if (layer.equals("service") != isInterface) {
                misplaced.add(SOURCES.relativize(file).toString());
            }
        }
        assertThat(misplaced).as("a class in service, or an interface in service/impl").isEmpty();
    }

    @Test
    void servicesReachEachOtherOnlyThroughTheirContracts() throws IOException {
        Path services = SOURCES.resolve("services");
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles(services)) {
            String owner = services.relativize(file).getName(0).toString();
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                Matcher reference = SERVICE_REFERENCE.matcher(lines.get(i));
                while (reference.find()) {
                    String packages = reference.group(2);
                    String layer = packages.substring(packages.lastIndexOf('.') + 1);
                    if (!reference.group(1).equals(owner) && !CONTRACT.contains(layer)) {
                        violations.add(where(file, i, lines));
                    }
                }
            }
        }
        assertThat(violations).as("a service reaching into another service's internals").isEmpty();
    }

    /**
     * Seed rows are read relative to the seeder's package, so a file left behind
     * by a move seeds nothing and says so only in a log line.
     */
    @Test
    void seedFilesSitInTheirServicesPackage() throws IOException {
        List<String> misplaced = new ArrayList<>();
        try (Stream<Path> files = Files.walk(RESOURCES)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Path folder = RESOURCES.relativize(file.getParent());
                boolean inService = folder.getNameCount() >= 2
                        && folder.getName(0).toString().equals("services")
                        && Files.isDirectory(SOURCES.resolve(folder.subpath(0, 2)));
                boolean readable = !file.toString().endsWith(".json")
                        || Files.isDirectory(SOURCES.resolve(folder));
                if (!inService || !readable) {
                    misplaced.add(RESOURCES.relativize(file).toString());
                }
            }
        }
        assertThat(misplaced).as("resources outside the package of the service that reads them").isEmpty();
    }

    /**
     * What a layer rule applies to: a feature.
     *
     * <p>A service that holds more than one feature gives each its own package
     * of layers — {@code taxonomy/category}, {@code taxonomy/tag} — so the
     * feature is the unit, not the service. A service with a single feature
     * keeps its layers directly, because {@code favorite/favorite/domain} says
     * nothing the shorter path does not.</p>
     */
    private static List<Path> features() throws IOException {
        List<Path> units = new ArrayList<>();
        for (Path service : services()) {
            List<Path> nested = new ArrayList<>();
            try (Stream<Path> folders = Files.list(service)) {
                for (Path folder : folders.filter(Files::isDirectory).sorted().toList()) {
                    if (!LAYERS.contains(folder.getFileName().toString())) {
                        nested.add(folder);
                    }
                }
            }
            units.addAll(nested.isEmpty() ? List.of(service) : nested);
        }
        return units;
    }

    /** The folders under {@code services/} that hold any Java: the services. */
    private static List<Path> services() throws IOException {
        try (Stream<Path> folders = Files.list(SOURCES.resolve("services"))) {
            List<Path> services = new ArrayList<>();
            for (Path folder : folders.filter(Files::isDirectory).sorted().toList()) {
                if (!javaFiles(folder).isEmpty()) {
                    services.add(folder);
                }
            }
            return services;
        }
    }

    /** The classes directly in one layer's folder, by file name. */
    private static List<String> classesIn(Path folder) throws IOException {
        return classFiles(folder).stream().map(file -> file.getFileName().toString()).toList();
    }

    private static List<Path> classFiles(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(file -> file.toString().endsWith(".java"))
                    .sorted()
                    .toList();
        }
    }

    private static List<Path> javaFiles(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(file -> file.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static String where(Path file, int index, List<String> lines) {
        return SOURCES.relativize(file) + ":" + (index + 1) + "  " + lines.get(index).trim();
    }
}
