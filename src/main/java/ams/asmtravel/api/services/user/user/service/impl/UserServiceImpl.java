package ams.asmtravel.api.services.user.user.service.impl;

import ams.asmtravel.api.services.user.user.service.IdentityService;
import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.user.user.config.UserProperties;
import ams.asmtravel.api.services.user.user.domain.TokenIdentity;
import ams.asmtravel.api.services.location.province.domain.ProvinceSummary;
import ams.asmtravel.api.services.location.province.service.ProvinceService;
import ams.asmtravel.api.services.user.user.domain.User;
import ams.asmtravel.api.services.user.user.domain.UserDeletingEvent;
import ams.asmtravel.api.services.user.user.domain.UserSummary;
import ams.asmtravel.api.services.user.user.dto.response.AuthorResponse;
import ams.asmtravel.api.services.user.user.dto.request.UserRequest;
import ams.asmtravel.api.services.user.user.dto.response.UserResponse;
import ams.asmtravel.api.services.user.user.exception.UserNotFoundException;
import ams.asmtravel.api.services.user.user.mapper.UserMapper;
import ams.asmtravel.api.services.user.user.repository.UserRepository;
import ams.asmtravel.api.services.user.user.service.UserService;
import ams.asmtravel.api.services.authorization.role.service.UserRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Only a verified Keycloak token counts as signed in. */
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    /** Where the person found for this request is kept, so it is looked up once. */
    private static final String RESOLVED_USER = UserServiceImpl.class.getName() + ".userId";

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final IdentityService identityService;
    private final UserRoleService userRoleService;
    private final ApplicationEventPublisher events;
    private final UserProperties properties;
    private final TransactionTemplate signInTransaction;
    private final TransactionTemplate signInOwnTransaction;
    private final String customerRealm;
    private final String backofficeRealm;
    /* Provinces record who made them, so their service is reached lazily. */
    private final ObjectProvider<ProvinceService> provinceService;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            IdentityService identityService,
            UserRoleService userRoleService,
            ApplicationEventPublisher events,
            UserProperties properties,
            PlatformTransactionManager transactionManager,
            ObjectProvider<ProvinceService> provinceService,
            @Value("${keycloak.realm:amstravel}") String customerRealm,
            @Value("${keycloak.backoffice-realm:amsadmin}") String backofficeRealm) {
        this.provinceService = provinceService;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.identityService = identityService;
        this.userRoleService = userRoleService;
        this.events = events;
        this.properties = properties;
        this.signInTransaction = new TransactionTemplate(transactionManager);
        this.signInOwnTransaction = new TransactionTemplate(transactionManager);
        this.signInOwnTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.customerRealm = customerRealm;
        this.backofficeRealm = backofficeRealm;
    }

    /** Keycloak first: only it knows an email taken by a customer who has not signed in here yet. */
    @Override
    @Transactional
    public UserResponse createNew(UserRequest request) {
        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot be empty");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email cannot be empty");
        }
        String email = normalizeEmail(request.email());
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        String fullName = request.fullName().trim();
        String subject = identityService.createAccount(email, fullName, request.password(), Boolean.TRUE.equals(request.temporaryPassword())).orElse(null);

        try {
            User user = new User();
            user.setEmail(email);
            user.setFullName(fullName);
            user.setPhone(blankToNull(request.phone()));
            user.setAvatarKey(blankToNull(request.avatarKey()));
            user.setHomeProvinceId(homeProvince(request.homeProvinceId()));
            user.setKeycloakId(parseUuid(subject));
            user = userRepository.save(user);
            if (subject != null) {
                identityService.link(user, customerRealm, subject);
            }
            userRoleService.replace(user, EnumSet.of(RoleCode.USER));
            userRepository.flush();
            log.info("User {} created (keycloak account: {})", user.getId(), subject == null ? "none" : subject);
            return respond(user);
        } catch (RuntimeException exception) {
            // Do not leave a sign-in behind for a profile that was never written.
            identityService.deleteAccountQuietly(subject);
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAll() {
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();
        Map<UUID, Set<RoleCode>> roles = userRoleService.rolesOf(users.stream().map(User::getId).toList());
        Map<UUID, String> provinces = provinceNames();
        return users.stream()
                .map(user -> userMapper.toResponse(user, roles.getOrDefault(user.getId(), Set.of()), provinces.get(user.getHomeProvinceId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return respond(getEntity(id));
    }

    /** Row first, then sign-in: a Keycloak refusal rolls the row back. */
    @Override
    @Transactional
    public UserResponse updateById(UUID id, UserRequest request) {
        User user = getEntity(id);
        if (request.fullName() != null) {
            if (request.fullName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot be empty");
            }
            user.setFullName(request.fullName().trim());
        }
        if (request.email() != null) {
            String email = normalizeEmail(request.email());
            userRepository.findByEmailIgnoreCase(email)
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
                    });
            user.setEmail(email);
        }
        // Empty means "none", as it does when someone is created.
        if (request.phone() != null) {
            user.setPhone(blankToNull(request.phone()));
        }
        if (request.avatarKey() != null) {
            user.setAvatarKey(blankToNull(request.avatarKey()));
        }
        if (request.homeProvinceId() != null) {
            user.setHomeProvinceId(homeProvince(request.homeProvinceId()));
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        User saved = userRepository.saveAndFlush(user);

        identityService.findProviderUserId(id, customerRealm).ifPresent(account ->
                identityService.updateAccount(account, saved.getEmail(), saved.getFullName(), saved.isActive()));
        return respond(saved);
    }

    /**
     * One transaction: other services clean up (or refuse), the row goes, then the sign-in.
     * Sign-in is last so a Keycloak failure rolls everything back.
     */
    @Override
    @Transactional
    public void deleteById(UUID id) {
        User user = getEntity(id);
        Optional<String> customerAccount = identityService.findProviderUserId(id, customerRealm);

        events.publishEvent(new UserDeletingEvent(id));
        userRepository.delete(user);
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This person has publishing history, so they cannot be "
                    + "deleted. Set them to Inactive instead.");
        }

        customerAccount.ifPresent(identityService::deleteAccount);
        log.info("User {} deleted (customer sign-in: {})", id, customerAccount.orElse("none"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findMine(UUID userId) {
        return respond(findActiveUser(userId));
    }

    @Override
    @Transactional
    public UserResponse updateMine(UUID userId, UserRequest request) {
        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot be empty");
        }
        User user = findActiveUser(userId);
        user.setFullName(request.fullName().trim());
        user.setAvatarKey(blankToNull(request.avatarKey()));
        if (request.phone() != null) {
            user.setPhone(blankToNull(request.phone()));
        }
        user.setHomeProvinceId(homeProvince(request.homeProvinceId()));
        return respond(userRepository.saveAndFlush(user));
    }

    @Override
    public UUID requireUserId(Authentication authentication) {
        return findUserId(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required"));
    }

    /**
     * Own transaction when called from a controller, so a new account's parallel first requests can race and retry.
     * Joins an outer transaction unless that one is read-only.
     */
    @Override
    public Optional<UUID> findUserId(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) {
            return Optional.empty();
        }
        Optional<UUID> resolved = resolvedUserId();
        if (resolved.isPresent()) {
            return resolved;
        }

        TokenIdentity identity = userMapper.toIdentity(
                token.getToken(), token.getAuthorities(), customerRealm, backofficeRealm);
        if (!identity.isComplete()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "The identity token must contain subject and email claims");
        }

        TransactionTemplate transaction = TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                ? signInOwnTransaction
                : signInTransaction;
        UUID userId;
        try {
            userId = transaction.execute(status -> synchronize(identity));
        } catch (DataIntegrityViolationException raced) {
            log.info("Another request made {}'s row first; reading it", identity.email());
            userId = transaction.execute(status -> synchronize(identity));
        }
        remember(userId);
        return Optional.ofNullable(userId);
    }

    @Override
    public Optional<UUID> resolvedUserId() {
        RequestAttributes request = RequestContextHolder.getRequestAttributes();
        Object userId = request == null ? null : request.getAttribute(RESOLVED_USER, RequestAttributes.SCOPE_REQUEST);
        return userId instanceof UUID id ? Optional.of(id) : Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public User getEntity(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return id != null && userRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummary> findSummary(UUID id) {
        return id == null
                ? Optional.empty()
                : userRepository.findById(id).map(user -> userMapper.toSummary(user, userRoleService.rolesOf(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, UserSummary> findSummaries(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Set<UUID> wanted = Set.copyOf(ids);
        Map<UUID, Set<RoleCode>> roles = userRoleService.rolesOf(wanted);
        return userRepository.findAllByIdIn(wanted).stream()
                .map(user -> userMapper.toSummary(user, roles.getOrDefault(user.getId(), Set.of())))
                .collect(Collectors.toMap(UserSummary::id, Function.identity()));
    }

    @Override
    public Set<UUID> findUserIdsHoldingAny(Set<RoleCode> roles) {
        return userRoleService.findUserIdsHoldingAny(roles);
    }

    /** Back-office accounts as id and name; a missing name falls back to the e-mail's name part. */
    @Override
    @Transactional(readOnly = true)
    public List<AuthorResponse> findAuthors() {
        Set<UUID> ids = userRoleService.findUserIdsHoldingAny(
                Set.of(RoleCode.PARTNER, RoleCode.CONTRIBUTOR, RoleCode.EDITOR, RoleCode.BDO, RoleCode.ADMIN));
        return userRepository.findAllByIdIn(ids).stream()
                .map(user -> new AuthorResponse(user.getId(), nameOf(user)))
                .sorted(Comparator.comparing(AuthorResponse::fullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static String nameOf(User user) {
        String name = user.getFullName();
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        String email = user.getEmail();
        if (email == null || email.isBlank()) {
            return "An account with no name";
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    @Override
    @Transactional
    public void mirrorBackofficeRoles(String keycloakId, Set<RoleCode> roles) {
        identityService.findUser(backofficeRealm, keycloakId).ifPresent(user -> {
            Set<RoleCode> wanted = EnumSet.noneOf(RoleCode.class);
            wanted.addAll(userRoleService.rolesOf(user.getId()));
            wanted.removeIf(RoleCode::isBackoffice);
            roles.stream().filter(RoleCode::isBackoffice).forEach(wanted::add);
            userRoleService.replace(user, wanted);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }

    /**
     * The row behind a verified sign-in, with roles and last sign-in refreshed.
     *
     * @throws ResponseStatusException 401 for an inactive person
     */
    private UUID synchronize(TokenIdentity identity) {
        User user = identityService.findUser(identity.provider(), identity.subject())
                .orElseGet(() -> link(identity));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account is unavailable");
        }

        Instant now = Instant.now();
        if (user.getLastLoginAt() == null
                || user.getLastLoginAt().isBefore(now.minus(properties.lastLoginPrecision()))) {
            user.setLastLoginAt(now);
            userRepository.saveAndFlush(user);
        }
        userRoleService.replace(user, rolesFrom(identity, userRoleService.rolesOf(user.getId())));
        return user.getId();
    }

    /** A new account: the person made for it in the dashboard, else the one with its email, else a new person. */
    private User link(TokenIdentity identity) {
        UUID keycloakId = parseUuid(identity.subject());
        User user = Optional.ofNullable(keycloakId)
                .flatMap(userRepository::findByKeycloakId)
                .or(() -> userRepository.findByEmailIgnoreCase(identity.email()))
                .orElseGet(() -> {
                    User created = new User();
                    created.setKeycloakId(keycloakId);
                    created.setEmail(identity.email());
                    created.setFullName(identity.name() == null ? identity.email() : identity.name());
                    return userRepository.save(created);
                });
        if (user.getKeycloakId() == null) {
            user.setKeycloakId(keycloakId);
        }
        identityService.link(user, identity.provider(), identity.subject());
        log.info("{} signed in through {} for the first time", identity.email(), identity.provider());
        return user;
    }

    /** Each realm's token updates only its own roles: back-office roles for staff, USER for customers. */
    private static Set<RoleCode> rolesFrom(TokenIdentity identity, Set<RoleCode> held) {
        Set<RoleCode> wanted = EnumSet.noneOf(RoleCode.class);
        wanted.addAll(held);
        if (identity.backoffice()) {
            wanted.removeIf(RoleCode::isBackoffice);
            identity.roles().stream().filter(RoleCode::isBackoffice).forEach(wanted::add);
        } else {
            wanted.add(RoleCode.USER);
        }
        return wanted;
    }

    private UserResponse respond(User user) {
        return userMapper.toResponse(user, userRoleService.rolesOf(user.getId()),
                user.getHomeProvinceId() == null ? null : provinceNames().get(user.getHomeProvinceId()));
    }

    /** A province that exists, or null for none; anything else is the caller's mistake. */
    private UUID homeProvince(UUID provinceId) {
        if (provinceId != null && !provinceService.getObject().exists(provinceId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such province");
        }
        return provinceId;
    }

    private Map<UUID, String> provinceNames() {
        Map<UUID, String> names = new HashMap<>();
        for (ProvinceSummary province : provinceService.getObject().findSummaries()) {
            names.put(province.id(), province.name());
        }
        return names;
    }

    private User findActiveUser(UUID id) {
        return userRepository.findById(id)
                .filter(User::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User account is unavailable"));
    }

    private void remember(UUID userId) {
        RequestAttributes request = RequestContextHolder.getRequestAttributes();
        if (request != null && userId != null) {
            request.setAttribute(RESOLVED_USER, userId, RequestAttributes.SCOPE_REQUEST);
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Keycloak ids are UUIDs; anything else (a test token's subject) has none. */
    private static UUID parseUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException notUuid) {
            return null;
        }
    }
}
