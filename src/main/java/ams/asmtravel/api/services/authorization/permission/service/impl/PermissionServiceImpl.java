package ams.asmtravel.api.services.authorization.permission.service.impl;

import ams.asmtravel.api.services.authorization.permission.domain.Permission;
import ams.asmtravel.api.services.authorization.permission.dto.request.PermissionRequest;
import ams.asmtravel.api.services.authorization.permission.dto.response.PermissionResponse;
import ams.asmtravel.api.services.authorization.permission.mapper.PermissionMapper;
import ams.asmtravel.api.services.authorization.permission.repository.PermissionRepository;
import ams.asmtravel.api.services.authorization.permission.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    public List<PermissionResponse> getAll(PermissionRequest request) {
        List<Permission> permissions = request == null || request.code() == null || request.code().isBlank()
                ? permissionRepository.findAllByOrderByCodeAsc()
                : permissionRepository.findByCodeIgnoreCase(request.code().trim());
        return permissions.stream().map(permissionMapper::toResponse).toList();
    }
}
