package ams.asmtravel.api.services.user.user.service.impl;

import ams.asmtravel.api.services.authorization.domain.Realms;
import ams.asmtravel.api.services.user.user.domain.AuthIdentity;
import ams.asmtravel.api.services.user.user.domain.User;
import ams.asmtravel.api.services.user.user.dto.request.IdentityRequest;
import ams.asmtravel.api.services.user.user.dto.response.IdentityResponse;
import ams.asmtravel.api.services.user.user.mapper.IdentityMapper;
import ams.asmtravel.api.services.user.user.repository.IdentityRepository;
import ams.asmtravel.api.services.user.user.service.IdentityService;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@code auth_identities} rows, and the customer realm via the {@code amstravelKeycloak} client. */
@Service
@Slf4j
public class IdentityServiceImpl implements IdentityService {

    private final IdentityRepository identityRepository;
    private final IdentityMapper identityMapper;
    private final ObjectProvider<Keycloak> keycloakProvider;

    public IdentityServiceImpl(
            IdentityRepository identityRepository,
            IdentityMapper identityMapper,
            @Qualifier("amstravelKeycloak") ObjectProvider<Keycloak> keycloakProvider) {
        this.identityRepository = identityRepository;
        this.identityMapper = identityMapper;
        this.keycloakProvider = keycloakProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdentityResponse> getAll(IdentityRequest request) {
        List<AuthIdentity> identities = request == null || request.userId() == null
                ? identityRepository.findAllByOrderByCreatedAtDesc()
                : identityRepository.findByUserIdOrderByCreatedAtDesc(request.userId());
        return identities.stream().map(identityMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUser(String provider, String providerUserId) {
        return identityRepository.findByProviderAndProviderUserId(provider, providerUserId).map(AuthIdentity::getUser);
    }

    @Override
    @Transactional
    public void link(User user, String provider, String providerUserId) {
        identityRepository.saveAndFlush(new AuthIdentity(user, provider, providerUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findProviderUserId(UUID userId, String provider) {
        return identityRepository.findFirstByUserIdAndProvider(userId, provider).map(AuthIdentity::getProviderUserId);
    }

    @Override
    public Optional<String> createAccount(String email, String name, String password, boolean temporary) {
        RealmResource realm = realm().orElse(null);
        if (realm == null) {
            log.warn("Keycloak administration is off; {} gets a profile but no sign-in", email);
            return Optional.empty();
        }

        UserRepresentation account = new UserRepresentation();
        // The realm sets registrationEmailAsUsername, so these must agree.
        account.setUsername(email);
        account.setEmail(email);
        applyName(account, name);
        account.setEnabled(true);
        // An administrator typed the address; the realm's own sign-up is what proves one.
        account.setEmailVerified(true);
        if (password != null && !password.isBlank()) {
            account.setCredentials(List.of(password(password, temporary)));
        }

        try (Response response = realm.users().create(account)) {
            if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "That e-mail address already has an account");
            }
            String id = CreatedResponseUtil.getCreatedId(response);
            log.info("Created customer account {} in {}", email, Realms.CUSTOMER);
            return Optional.of(id);
        } catch (WebApplicationException | ProcessingException exception) {
            throw translate(exception);
        }
    }

    @Override
    public void updateAccount(String keycloakId, String email, String name, boolean enabled) {
        RealmResource realm = keycloakId == null || keycloakId.isBlank() ? null : realm().orElse(null);
        if (realm == null) {
            return;
        }
        try {
            UserResource resource = realm.users().get(keycloakId);
            UserRepresentation account = resource.toRepresentation();
            // The realm keeps the username equal to the e-mail, so the username follows on its own.
            account.setEmail(email);
            account.setFirstName(null);
            account.setLastName(null);
            applyName(account, name);
            account.setEnabled(enabled);
            resource.update(account);
            log.info("Updated customer account {} in {}", keycloakId, Realms.CUSTOMER);
        } catch (NotFoundException missing) {
            log.info("{} is not a customer account; only the profile row changed", keycloakId);
        } catch (WebApplicationException | ProcessingException exception) {
            throw translate(exception);
        }
    }

    @Override
    public void deleteAccount(String keycloakId) {
        RealmResource realm = keycloakId == null || keycloakId.isBlank() ? null : realm().orElse(null);
        if (realm == null) {
            return;
        }
        try (Response response = realm.users().delete(keycloakId)) {
            int status = response.getStatus();
            if (status == Response.Status.NOT_FOUND.getStatusCode()) {
                log.info("{} had no customer account to remove", keycloakId);
            } else if (status >= 300) {
                log.warn("Keycloak refused to remove customer account {}: {}", keycloakId, status);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Keycloak did not remove the sign-in", null);
            } else {
                log.info("Removed customer account {} from {}", keycloakId, Realms.CUSTOMER);
            }
        } catch (WebApplicationException | ProcessingException exception) {
            throw translate(exception);
        }
    }

    @Override
    public void deleteAccountQuietly(String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) {
            return;
        }
        try {
            realm().ifPresent(realm -> realm.users().delete(keycloakId).close());
        } catch (RuntimeException exception) {
            // Swallowed: another failure is being reported, and an orphaned sign-in is recoverable.
            log.warn("Could not roll back customer account {}: {}", keycloakId, exception.getMessage());
        }
    }

    /** Empty when {@code app.keycloak.admin.enabled} is false, and in tests. */
    private Optional<RealmResource> realm() {
        Keycloak keycloak = keycloakProvider.getIfAvailable();
        return keycloak == null
                ? Optional.empty()
                : Optional.of(keycloak.realm(Realms.CUSTOMER));
    }

    /** Whatever is before the first space is the given name; the rest follows. */
    private static void applyName(UserRepresentation account, String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        String trimmed = name.trim();
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            account.setFirstName(trimmed);
            return;
        }
        account.setFirstName(trimmed.substring(0, space));
        account.setLastName(trimmed.substring(space + 1).trim());
    }

    private static CredentialRepresentation password(String value, boolean temporary) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(value);
        credential.setTemporary(temporary);
        return credential;
    }

    /** Keycloak's status, translated. Its message is not passed on: it can quote a password back. */
    private static RuntimeException translate(RuntimeException exception) {
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
            case 409 -> new ResponseStatusException(HttpStatus.CONFLICT, "That e-mail address already has an account");
            case 400 -> new ResponseStatusException(HttpStatus.CONFLICT, "Keycloak rejected those details — check the password policy");
            default -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Keycloak did not answer", exception);
        };
    }
}
