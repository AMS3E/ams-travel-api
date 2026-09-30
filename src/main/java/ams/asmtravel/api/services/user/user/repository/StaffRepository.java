package ams.asmtravel.api.services.user.user.repository;

import ams.asmtravel.api.services.authorization.domain.Realms;
import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.user.user.config.StaffProperties;
import ams.asmtravel.api.services.user.user.domain.BackofficeUser;
import ams.asmtravel.api.services.user.user.exception.StaffAccountNotFoundException;
import ams.asmtravel.api.services.user.user.mapper.StaffMapper;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Staff accounts and roles via the Keycloak Admin Client. Not cached: Keycloak is the source of truth. */
@Repository
@RequiredArgsConstructor
@Slf4j
public class StaffRepository {

    private final ObjectProvider<Keycloak> keycloakProvider;
    private final StaffMapper backofficeUserMapper;
    private final StaffProperties properties;

    private RealmResource realm() {
        Keycloak keycloak = keycloakProvider.getIfAvailable();
        if (keycloak == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, 
                    "Keycloak administration is not configured for this API", null);
        }
        return keycloak.realm(Realms.BACKOFFICE);
    }

    /** Asked once per back-office role rather than once per account. */
    public List<BackofficeUser> findBackofficeUsers() {
        RealmResource realm = realm();
        Map<String, UserRepresentation> users = new LinkedHashMap<>();
        Map<String, Set<RoleCode>> roles = new LinkedHashMap<>();

        try {
            for (RoleCode role : backofficeRoles()) {
                Collection<UserRepresentation> members =
                        realm.roles().get(role.name()).getUserMembers(0, properties.roleMemberLimit());
                for (UserRepresentation member : members) {
                    users.putIfAbsent(member.getId(), member);
                    roles.computeIfAbsent(member.getId(), key -> EnumSet.noneOf(RoleCode.class)).add(role);
                }
            }
        } catch (WebApplicationException | ProcessingException exception) {
            throw translate(exception);
        }

        return users.values().stream()
                .map(user -> backofficeUserMapper.toDomain(user, roles.get(user.getId())))
                .sorted((left, right) -> left.username().compareToIgnoreCase(right.username()))
                .toList();
    }

    public List<BackofficeUser> search(String query) {
        RealmResource realm = realm();
        List<BackofficeUser> found = new ArrayList<>();
        try {
            for (UserRepresentation user : realm.users().search(query, 0, properties.searchLimit())) {
                found.add(backofficeUserMapper.toDomain(user, backofficeRolesOf(realm, user.getId())));
            }
        } catch (WebApplicationException | ProcessingException exception) {
            throw translate(exception);
        }
        return found;
    }

    public BackofficeUser findById(String id) {
        RealmResource realm = realm();
        UserRepresentation user;
        try {
            user = realm.users().get(id).toRepresentation();
        } catch (NotFoundException exception) {
            throw new StaffAccountNotFoundException();
        } catch (WebApplicationException | ProcessingException exception) {
            throw translate(exception);
        }
        return backofficeUserMapper.toDomain(user, backofficeRolesOf(realm, id));
    }

    public BackofficeUser create(BackofficeUser account, String password, boolean temporaryPassword) {
        RealmResource realm = realm();
        // Check the role before creating an account that must be able to sign in.
        List<RoleRepresentation> initialRoles = representations(realm, account.roles());

        UserRepresentation user = backofficeUserMapper.toRepresentation(account);
        user.setCredentials(List.of(password(password, temporaryPassword)));

        String id;
        try (Response response = realm.users().create(user)) {
            if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "That username or e-mail address is already taken");
            }
            id = CreatedResponseUtil.getCreatedId(response);
        } catch (WebApplicationException | ProcessingException exception) {
            throw translate(exception);
        }

        // From here the account exists; a failed role mapping is recoverable, a lost account is not.
        try {
            realm.users().get(id).roles().realmLevel().add(initialRoles);
        } catch (WebApplicationException | ProcessingException exception) {
            throw translate(exception);
        }
        return findById(id);
    }

    public void grantRoles(String id, Set<RoleCode> roles) {
        if (roles.isEmpty()) {
            return;
        }
        RealmResource realm = realm();
        try {
            realm.users().get(id).roles().realmLevel().add(representations(realm, roles));
        } catch (WebApplicationException | ProcessingException exception) {
            throw translate(exception);
        }
    }

    public void revokeRoles(String id, Set<RoleCode> roles) {
        if (roles.isEmpty()) {
            return;
        }
        RealmResource realm = realm();
        try {
            realm.users().get(id).roles().realmLevel().remove(representations(realm, roles));
        } catch (WebApplicationException | ProcessingException exception) {
            throw translate(exception);
        }
    }

    private static List<RoleCode> backofficeRoles() {
        return EnumSet.allOf(RoleCode.class).stream().filter(RoleCode::isBackoffice).toList();
    }

    /** Mapping a role takes its representation, not its name. */
    private List<RoleRepresentation> representations(RealmResource realm, Set<RoleCode> roles) {
        try {
            return roles.stream()
                    .map(role -> realm.roles().get(role.name()).toRepresentation())
                    .toList();
        } catch (WebApplicationException | ProcessingException exception) {
            throw translate(exception);
        }
    }

    /** Composites are not expanded: an administrator reads as ADMIN only. */
    private Set<RoleCode> backofficeRolesOf(RealmResource realm, String id) {
        Set<RoleCode> held = EnumSet.noneOf(RoleCode.class);
        try {
            for (RoleRepresentation role : realm.users().get(id).roles().realmLevel().listAll()) {
                RoleCode.parse(role.getName()).filter(RoleCode::isBackoffice).ifPresent(held::add);
            }
        } catch (WebApplicationException | ProcessingException exception) {
            throw translate(exception);
        }
        return held;
    }

    private CredentialRepresentation password(String value, boolean temporary) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(value);
        credential.setTemporary(temporary);
        return credential;
    }

    /**
     * Translates Keycloak's status; its message is dropped because it can echo a password.
     * A {@link ProcessingException} means Keycloak was not reached.
     */
    private RuntimeException translate(RuntimeException exception) {
        if (exception instanceof ProcessingException) {
            log.warn("Keycloak could not be reached at all", exception);
            return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Keycloak did not answer", exception);
        }
        if (!(exception instanceof WebApplicationException webException)) {
            return exception;
        }
        int status = webException.getResponse() == null ? 0 : webException.getResponse().getStatus();
        log.warn("Keycloak admin API answered {}", status);
        return switch (status) {
            case 404 -> new StaffAccountNotFoundException();
            case 409 -> new ResponseStatusException(HttpStatus.CONFLICT, "That username or e-mail address is already taken");
            case 400 -> new ResponseStatusException(HttpStatus.CONFLICT, "Keycloak rejected those details — check the password policy");
            default -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Keycloak did not answer", exception);
        };
    }
}
