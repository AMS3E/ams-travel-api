package ams.asmtravel.api.services.user.user.service.impl;

import ams.asmtravel.api.services.media.service.MediaService;
import ams.asmtravel.api.services.user.user.domain.BackofficeUser;
import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.user.user.domain.StaffProfile;
import ams.asmtravel.api.services.user.user.dto.request.StaffProfileRequest;
import ams.asmtravel.api.services.user.user.dto.request.StaffRequest;
import ams.asmtravel.api.services.user.user.dto.response.StaffResponse;
import ams.asmtravel.api.services.user.user.mapper.StaffMapper;
import ams.asmtravel.api.services.user.user.repository.StaffRepository;
import ams.asmtravel.api.services.user.user.service.StaffService;
import ams.asmtravel.api.services.user.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Staff account workflows and role policy; the repository owns every Keycloak call. */
@Service
@RequiredArgsConstructor
@Slf4j
public class StaffServiceImpl implements StaffService {

    private final StaffRepository keycloakUserRepository;
    private final StaffMapper backofficeUserMapper;
    private final UserService userService;
    private final MediaService mediaService;

    @Override
    public List<StaffResponse> findBackofficeUsers() {
        return respond(keycloakUserRepository.findBackofficeUsers());
    }

    @Override
    public List<StaffResponse> search(String query) {
        return respond(keycloakUserRepository.search(query));
    }

    @Override
    public StaffResponse findById(String id) {
        return respond(keycloakUserRepository.findById(id));
    }

    @Override
    public StaffResponse create(StaffRequest request) {
        if (isBlank(request.username()) || isBlank(request.email()) || isBlank(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A new account needs a username, an e-mail address and a password");
        }
        if (request.roles() == null || request.roles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Give the new account at least one back-office role");
        }
        requireBackoffice(request.roles());
        BackofficeUser created = keycloakUserRepository.create(
                backofficeUserMapper.toDomain(request), request.password(), Boolean.TRUE.equals(request.temporaryPassword()));
        log.info("Created back-office account {} as {}", request.username(), request.roles());
        return respond(created);
    }

    @Override
    public StaffResponse update(String id, StaffProfileRequest request) {
        // Checked here so nobody locks themselves out.
        if (id.equals(callerSubject()) && !request.enabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot disable your own account — ask another administrator");
        }
        String firstName = request.firstName().trim();
        String lastName = request.lastName() == null ? "" : request.lastName().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        BackofficeUser updated = keycloakUserRepository.update(id, firstName, lastName, email, request.enabled());
        // Keycloak is the account; the person here carries what Keycloak does not (phone, photo) and mirrors the rest.
        userService.mirrorStaffProfile(id, email, (firstName + " " + lastName).trim(), request.phone(), request.avatarKey());
        log.info("Account {} edited: {}", id, request.enabled() ? "may sign in" : "disabled");
        return respond(updated);
    }

    @Override
    public StaffResponse setRoles(String id, Set<RoleCode> roles) {
        if (roles == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Send the complete set of roles the account should hold");
        }
        requireBackoffice(roles);
        Set<RoleCode> current = keycloakUserRepository.findById(id).roles();
        Set<RoleCode> wanted = copy(roles);

        // Checked here so no caller can remove ADMIN from their own account.
        if (id.equals(callerSubject()) && current.contains(RoleCode.ADMIN) && !wanted.contains(RoleCode.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot take ADMIN off your own account — ask another administrator");
        }

        Set<RoleCode> toGrant = copy(wanted);
        toGrant.removeAll(current);
        Set<RoleCode> toRevoke = copy(current);
        toRevoke.removeAll(wanted);

        keycloakUserRepository.grantRoles(id, toGrant);
        keycloakUserRepository.revokeRoles(id, toRevoke);
        // The next token would do this too; mirror now so screens need not wait.
        userService.mirrorBackofficeRoles(id, wanted);
        log.info("Account {} now holds {}", id, wanted);
        return findById(id);
    }

    /** The account with the phone and photo from the person's profile here. */
    private StaffResponse respond(BackofficeUser user) {
        return respond(List.of(user)).getFirst();
    }

    /** One profile lookup for the whole list. */
    private List<StaffResponse> respond(List<BackofficeUser> users) {
        Map<UUID, StaffProfile> profiles = userService.findStaffProfiles(
                users.stream().map(user -> parseUuid(user.id())).filter(Objects::nonNull).toList());
        return users.stream().map(user -> {
            StaffProfile profile = profiles.get(parseUuid(user.id()));
            return backofficeUserMapper.toBackofficeUserResponse(user, profile,
                    profile == null ? null : mediaService.url(profile.avatarKey()));
        }).toList();
    }

    private static UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /** USER is the customer realm's; it is not handed out here. */
    private static void requireBackoffice(Set<RoleCode> roles) {
        if (roles.stream().anyMatch(role -> !role.isBackoffice())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PARTNER, CONTRIBUTOR, EDITOR, BDO and ADMIN can be assigned here");
        }
    }

    private static Set<RoleCode> copy(Set<RoleCode> roles) {
        Set<RoleCode> copy = EnumSet.noneOf(RoleCode.class);
        copy.addAll(roles);
        return copy;
    }

    private static String callerSubject() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof JwtAuthenticationToken jwt ? jwt.getToken().getSubject() : null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
