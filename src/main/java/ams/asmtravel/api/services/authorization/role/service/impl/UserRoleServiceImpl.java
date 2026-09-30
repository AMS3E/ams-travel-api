package ams.asmtravel.api.services.authorization.role.service.impl;

import ams.asmtravel.api.services.authorization.role.domain.Role;
import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.authorization.role.domain.UserRole;
import ams.asmtravel.api.services.authorization.role.dto.request.UserRoleRequest;
import ams.asmtravel.api.services.authorization.role.dto.response.UserRoleResponse;
import ams.asmtravel.api.services.authorization.role.mapper.UserRoleMapper;
import ams.asmtravel.api.services.authorization.role.repository.UserRoleRepository;
import ams.asmtravel.api.services.authorization.role.service.RoleService;
import ams.asmtravel.api.services.authorization.role.service.UserRoleService;
import ams.asmtravel.api.services.user.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final UserRoleMapper userRoleMapper;
    private final RoleService roleService;

    @Override
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getAll(UserRoleRequest request) {
        List<UserRole> held = request == null || request.userId() == null
                ? userRoleRepository.findAllByOrderByAssignedAtDesc()
                : userRoleRepository.findByIdUserId(request.userId());
        return held.stream().map(userRoleMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<RoleCode> rolesOf(UUID userId) {
        return userId == null ? Set.of() : codes(userRoleRepository.findByIdUserId(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Set<RoleCode>> rolesOf(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<UserRole>> byUser = userRoleRepository.findByIdUserIdIn(Set.copyOf(userIds)).stream()
                .collect(Collectors.groupingBy(held -> held.getId().getUserId()));
        Map<UUID, Set<RoleCode>> roles = new HashMap<>();
        byUser.forEach((userId, held) -> roles.put(userId, codes(held)));
        return roles;
    }

    /** Syncs the rows; flushes at once so a race fails here (retryable), not at commit. */
    @Override
    @Transactional
    public boolean replace(User user, Set<RoleCode> wanted) {
        List<UserRole> rows = userRoleRepository.findByIdUserId(user.getId());
        Set<RoleCode> held = codes(rows);
        if (held.equals(wanted)) {
            return false;
        }
        rows.stream()
                .filter(row -> RoleCode.parse(row.getRole().getCode()).map(code -> !wanted.contains(code)).orElse(false))
                .forEach(userRoleRepository::delete);

        Set<RoleCode> missing = EnumSet.noneOf(RoleCode.class);
        missing.addAll(wanted);
        missing.removeAll(held);
        if (!missing.isEmpty()) {
            Map<String, Role> byCode = roleService.findByCodes(missing).stream()
                    .collect(Collectors.toMap(Role::getCode, Function.identity()));
            for (RoleCode code : missing) {
                Role role = byCode.get(code.name());
                if (role == null) {
                    log.warn("auth.roles has no {} row; the database changelog has not run", code);
                    continue;
                }
                userRoleRepository.save(new UserRole(user, role));
            }
        }
        userRoleRepository.flush();
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> findUserIdsHoldingAny(Set<RoleCode> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(userRoleRepository.findUserIdsHoldingAny(roles.stream().map(Enum::name).toList()));
    }

    private static Set<RoleCode> codes(Collection<UserRole> rows) {
        Set<RoleCode> codes = EnumSet.noneOf(RoleCode.class);
        for (UserRole row : rows) {
            RoleCode.parse(row.getRole().getCode()).ifPresent(codes::add);
        }
        return codes;
    }
}
