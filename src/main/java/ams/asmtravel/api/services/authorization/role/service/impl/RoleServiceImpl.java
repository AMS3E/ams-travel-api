package ams.asmtravel.api.services.authorization.role.service.impl;

import ams.asmtravel.api.services.authorization.role.domain.Role;
import ams.asmtravel.api.services.authorization.role.domain.RoleCode;
import ams.asmtravel.api.services.authorization.role.dto.request.RoleRequest;
import ams.asmtravel.api.services.authorization.role.dto.response.RoleResponse;
import ams.asmtravel.api.services.authorization.role.mapper.RoleMapper;
import ams.asmtravel.api.services.authorization.role.repository.RoleRepository;
import ams.asmtravel.api.services.authorization.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    /** Lowest first; unknown roles go last. */
    @Override
    public List<RoleResponse> getAll(RoleRequest request) {
        List<Role> roles = request == null || request.code() == null || request.code().isBlank()
                ? roleRepository.findAllByOrderByCreatedAtAsc()
                : roleRepository.findByCodeIgnoreCase(request.code().trim());
        return roles.stream()
                .sorted(Comparator.comparingInt(RoleServiceImpl::rank))
                .map(roleMapper::toResponse)
                .toList();
    }

    @Override
    public List<Role> findByCodes(Collection<RoleCode> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return roleRepository.findByCodeIn(codes.stream().map(Enum::name).toList());
    }

    private static int rank(Role role) {
        return RoleCode.parse(role.getCode()).map(Enum::ordinal).orElse(Integer.MAX_VALUE);
    }
}
