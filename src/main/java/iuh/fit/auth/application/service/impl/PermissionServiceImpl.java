package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.port.persistence.PermissionRepository;
import iuh.fit.auth.application.service.PermissionService;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.PageFilter;

import java.util.UUID;

public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;

    public PermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public Permission getPermissionById(String permissionId) throws ServiceException {
        return permissionRepository.findById(UUID.fromString(permissionId))
                .orElseThrow(() -> new ServiceException("Quyền hạn không tồn tại"));
    }

    @Override
    public PageResult<Permission> getAllPermissions(PageFilter filter) {
        return permissionRepository.findAll(filter);
    }

    @Override
    public boolean existsPermission(String apiPath, String module, String method) {
        return permissionRepository.existsByApiPathAndModuleAndMethod(apiPath, module, method);
    }
}
