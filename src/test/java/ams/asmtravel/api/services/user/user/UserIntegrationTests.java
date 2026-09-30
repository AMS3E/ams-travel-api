package ams.asmtravel.api.services.user.user;

import ams.asmtravel.api.services.user.user.domain.User;
import ams.asmtravel.api.services.user.user.domain.UserStatus;
import ams.asmtravel.api.services.user.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * People: the row a first sign-in makes, the roles each sign-in mirrors, a
 * person's own profile and preferences, and the Users screen. Keycloak
 * administration is off in tests, so these are the database's half.
 */
@SpringBootTest(properties = "app.security.oauth2.enabled=true")
@AutoConfigureMockMvc
@Transactional
class UserIntegrationTests {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;

    @Test
    void anonymousCallersAreTurnedAwayFromTheirOwnData() throws Exception {
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/preferences/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void aFirstSignInMakesThePersonAndMirrorsTheirRole() throws Exception {
        String subject = UUID.randomUUID().toString();

        mvc.perform(get("/api/v1/users/me").with(customer(subject, "First@Sign.In")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("first@sign.in"))
                .andExpect(jsonPath("$.fullName").value("Visitor"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.lastLoginAt").isNotEmpty());

        User person = users.findByEmailIgnoreCase("first@sign.in").orElseThrow();
        assertThat(person.getKeycloakId()).isEqualTo(UUID.fromString(subject));
        mvc.perform(get("/api/v1/identities").param("userId", person.getId().toString()).with(manager()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].provider").value("amstravel"))
                .andExpect(jsonPath("$[0].providerUserId").value(subject));
    }

    /** A second sign-in finds the same person; last_login_at is not rewritten on every request. */
    @Test
    void aSecondSignInFindsTheSamePerson() throws Exception {
        String subject = UUID.randomUUID().toString();
        mvc.perform(get("/api/v1/users/me").with(customer(subject, "again@sign.in"))).andExpect(status().isOk());
        User person = users.findByEmailIgnoreCase("again@sign.in").orElseThrow();
        Instant firstLogin = person.getLastLoginAt();

        mvc.perform(get("/api/v1/users/me").with(customer(subject, "again@sign.in")))
                .andExpect(jsonPath("$.id").value(person.getId().toString()));

        assertThat(users.count()).isEqualTo(users.findAll().stream().map(User::getEmail).distinct().count());
        assertThat(users.findById(person.getId()).orElseThrow().getLastLoginAt()).isEqualTo(firstLogin);
    }

    /**
     * The staff realm speaks for back-office roles: the roles a token carries
     * replace the ones held, and a customer identity with the same email is
     * the same person, keeping USER.
     */
    @Test
    void staffRolesFollowTheLatestStaffToken() throws Exception {
        mvc.perform(get("/api/v1/users/me").with(customer(UUID.randomUUID().toString(), "both@realms.test")))
                .andExpect(status().isOk());
        String staffSubject = UUID.randomUUID().toString();

        mvc.perform(get("/api/v1/users/me").with(staff(staffSubject, "both@realms.test", "EDITOR")))
                .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.contains("USER", "EDITOR")));
        mvc.perform(get("/api/v1/users/me").with(staff(staffSubject, "both@realms.test", "BDO")))
                .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.contains("USER", "BDO")));

        User person = users.findByEmailIgnoreCase("both@realms.test").orElseThrow();
        mvc.perform(get("/api/v1/identities").param("userId", person.getId().toString()).with(manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].provider", containsInAnyOrder("amstravel", "amsadmin")))
                .andExpect(jsonPath("$[*].userId", everyItem(org.hamcrest.Matchers.is(person.getId().toString()))));
        mvc.perform(get("/api/v1/user-roles").param("userId", person.getId().toString()).with(manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].roleCode", containsInAnyOrder("USER", "BDO")));
    }

    /**
     * A list of somebody else's rows says who wrote each one, so the desks that
     * read those lists read the names — and nothing else about the accounts.
     */
    @Test
    void theDesksThatReadOtherPeoplesRowsReadTheAuthorsNames() throws Exception {
        String bdoSubject = UUID.randomUUID().toString();
        mvc.perform(get("/api/v1/users/me").with(staff(bdoSubject, "author@bdo.test", "BDO"))).andExpect(status().isOk());
        RequestPostProcessor bdo = staff(bdoSubject, "author@bdo.test", "BDO", "PERM_PARTNER_PUBLISH_APPROVE");
        mvc.perform(get("/api/v1/users/authors").with(bdo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isNotEmpty())
                .andExpect(jsonPath("$[*].fullName", everyItem(not(emptyOrNullString()))));

        // Names only: the accounts themselves stay with USER_MANAGE, and a partner reads neither.
        mvc.perform(get("/api/v1/users").with(bdo)).andExpect(status().isForbidden());
        RequestPostProcessor partner = staff(UUID.randomUUID().toString(), "own@partner.test", "PARTNER",
                "PERM_LISTING_EDIT_OWN");
        mvc.perform(get("/api/v1/users/authors").with(partner)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users/authors")).andExpect(status().isUnauthorized());
    }

    /** Which accounts and roles someone holds is personal data too: USER_MANAGE's to read. */
    @Test
    void identitiesAndHeldRolesAreReadByUserManagersOnly() throws Exception {
        mvc.perform(get("/api/v1/identities")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/user-roles")).andExpect(status().isUnauthorized());
        RequestPostProcessor editor = staff(UUID.randomUUID().toString(), "ed2@staff.test", "EDITOR");
        mvc.perform(get("/api/v1/identities").with(editor)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/user-roles").with(editor)).andExpect(status().isForbidden());
    }

    @Test
    void anInactivePersonCannotSignIn() throws Exception {
        User person = person("inactive@sign.in");
        person.setStatus(UserStatus.INACTIVE);
        users.save(person);

        mvc.perform(get("/api/v1/users/me").with(customer(UUID.randomUUID().toString(), "inactive@sign.in")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aPersonEditsTheirNameAndPicture() throws Exception {
        String subject = UUID.randomUUID().toString();
        String picture = "avatar/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".png";

        mvc.perform(put("/api/v1/users/me").with(customer(subject, "me@profile.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"  Dara Kim \",\"avatarKey\":\"" + picture + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Dara Kim"))
                .andExpect(jsonPath("$.avatarKey").value(picture))
                .andExpect(jsonPath("$.avatarUrl").value("http://localhost:8080/api/v1/media/files/" + picture));

        mvc.perform(put("/api/v1/users/me").with(customer(subject, "me@profile.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Dara Kim\",\"avatarKey\":\"https://elsewhere.test/me.png\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/v1/users/me").with(customer(subject, "me@profile.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Dara Kim\",\"avatarKey\":\"\"}"))
                .andExpect(jsonPath("$.avatarKey").isEmpty())
                .andExpect(jsonPath("$.avatarUrl").isEmpty());
    }

    @Test
    void preferencesStartAtTheDefaultsAndKeepWhatIsSaved() throws Exception {
        RequestPostProcessor me = customer(UUID.randomUUID().toString(), "prefs@profile.test");

        mvc.perform(get("/api/v1/preferences/me").with(me))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredLanguage").value("en"))
                .andExpect(jsonPath("$.preferredCurrency").value("USD"))
                .andExpect(jsonPath("$.theme").value("SYSTEM"))
                .andExpect(jsonPath("$.notificationEmail").value(true))
                .andExpect(jsonPath("$.updatedAt").isEmpty());

        mvc.perform(put("/api/v1/preferences/me").with(me)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"preferredLanguage":"km","preferredCurrency":"KHR","preferredRegionId":null,
                                 "notificationEmail":false,"notificationPush":true,"theme":"DARK"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredLanguage").value("km"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mvc.perform(get("/api/v1/preferences/me").with(me))
                .andExpect(jsonPath("$.preferredCurrency").value("KHR"))
                .andExpect(jsonPath("$.notificationEmail").value(false))
                .andExpect(jsonPath("$.theme").value("DARK"));

        mvc.perform(put("/api/v1/preferences/me").with(me)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredLanguage\":\"English\",\"preferredCurrency\":\"usd\","
                                + "\"notificationEmail\":true,\"notificationPush\":true,\"theme\":\"DARK\"}"))
                .andExpect(status().isBadRequest());
    }

    /* --- the Users screen -------------------------------------------------- */

    @Test
    void anAdministratorCreatesEditsAndDeletesAPerson() throws Exception {
        String created = mvc.perform(post("/api/v1/users").with(manager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Sokha Chan\",\"email\":\"Sokha@Example.test\",\"phone\":\"012345678\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("sokha@example.test"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(created, "$.id");
        String path = "/api/v1/users/" + id;

        mvc.perform(put(path).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Sokha C.\",\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Sokha C."))
                .andExpect(jsonPath("$.phone").value("012345678"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
        mvc.perform(put(path).with(manager()).contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"  \"}"))
                .andExpect(jsonPath("$.phone").isEmpty());

        mvc.perform(get("/api/v1/users").with(manager()))
                .andExpect(jsonPath("$[*].email", hasItem("sokha@example.test")));

        mvc.perform(delete(path).with(manager())).andExpect(status().isNoContent());
        mvc.perform(get(path).with(manager())).andExpect(status().isNotFound());
    }

    @Test
    void anEditCannotTakeAnotherPersonsEmailOrBlankTheName() throws Exception {
        person("taken@users.test");
        String path = "/api/v1/users/" + person("wants-it@users.test").getId();

        mvc.perform(put(path).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"TAKEN@users.test\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put(path).with(manager()).contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(path).with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-address\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/users").with(manager()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Again\",\"email\":\"taken@users.test\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void anUnknownPersonCannotBeEditedOrDeleted() throws Exception {
        String path = "/api/v1/users/" + UUID.randomUUID();
        mvc.perform(put(path).with(manager()).contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Nobody\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete(path).with(manager())).andExpect(status().isNotFound());
    }

    /** Anyone's account is personal data: USER_MANAGE's, reads included. */
    @Test
    void onlyAUserManagerMayReadOrChangePeople() throws Exception {
        User person = person("protected@users.test");
        String path = "/api/v1/users/" + person.getId();

        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"X\"}"))
                .andExpect(status().isUnauthorized());
        for (String role : new String[] {"USER", "PARTNER", "EDITOR", "BDO"}) {
            RequestPostProcessor caller = staff(UUID.randomUUID().toString(), role.toLowerCase() + "@staff.test", role);
            mvc.perform(get(path).with(caller)).andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/users").with(caller)).andExpect(status().isForbidden());
            mvc.perform(delete(path).with(caller)).andExpect(status().isForbidden());
        }
        assertThat(users.findById(person.getId())).get().extracting(User::getFullName).isEqualTo("Someone");
    }

    /** The mirror of the realm: six roles, and ADMIN carrying every permission. */
    @Test
    void rolesAndPermissionsAreReadByThoseWhoAssignThem() throws Exception {
        mvc.perform(get("/api/v1/roles").with(manager()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].code").value("USER"))
                .andExpect(jsonPath("$[5].code").value("ADMIN"))
                .andExpect(jsonPath("$[5].permissions", hasSize(24)))
                .andExpect(jsonPath("$[1].permissions", hasItem("PERM_MEDIA_UPLOAD")));
        mvc.perform(get("/api/v1/permissions").with(manager()))
                .andExpect(jsonPath("$", hasSize(24)));
        mvc.perform(get("/api/v1/roles").param("code", "editor").with(manager()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("EDITOR"));
        mvc.perform(get("/api/v1/permissions").param("code", "PERM_MEDIA_UPLOAD").with(manager()))
                .andExpect(jsonPath("$", hasSize(1)));

        mvc.perform(get("/api/v1/roles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/roles").with(staff(UUID.randomUUID().toString(), "ed@staff.test", "EDITOR")))
                .andExpect(status().isForbidden());
    }

    /**
     * The People screen changes a role by sending only {@code roles}. The body
     * must be read as it is — here it then reaches Keycloak administration,
     * which is off in tests, rather than stopping at "Failed to read request".
     */
    @Test
    void aRoleChangeSendsOnlyTheRoles() throws Exception {
        mvc.perform(put("/api/v1/staff/" + UUID.randomUUID() + "/roles").with(manager())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[\"EDITOR\"]}"))
                .andExpect(status().isServiceUnavailable());
    }

    /* ---------------------------------------------------------------------- */

    private RequestPostProcessor customer(String subject, String email) {
        return jwt().jwt(token -> token.subject(subject).claim("email", email).claim("name", "Visitor"))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private RequestPostProcessor staff(String subject, String email, String... roles) {
        return jwt().jwt(token -> token.subject(subject).claim("email", email).claim("name", "Staff"))
                .authorities(Arrays.stream(roles)
                        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                        .toList());
    }

    private RequestPostProcessor manager() {
        return jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("email", "manager@staff.test"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_PERM_USER_MANAGE"),
                        new SimpleGrantedAuthority("ROLE_PERM_ROLE_ASSIGN"));
    }

    private User person(String email) {
        User user = new User();
        user.setFullName("Someone");
        user.setEmail(email);
        user.setLastLoginAt(Instant.now().minus(1, ChronoUnit.DAYS));
        return users.save(user);
    }
}
