package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.port.persistence.PermissionRepository;
import iuh.fit.auth.application.service.PermissionService;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.permission.CreatePermissionRequestCommand;
import iuh.fit.auth.application.dto.request.permission.UpdatePermissionRequestCommand;

import java.util.UUID;

public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;

    public PermissionServiceImpl(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public Permission createPermission(CreatePermissionRequestCommand command) throws ServiceException {
        Permission permission = new Permission();
        permission.setName(command.name().toUpperCase().replace(" ", "_"));
        permission.setApiPath(command.apiPath());
        permission.setMethod(command.method().toUpperCase());
        permission.setModule(command.module().toUpperCase());

        return permissionRepository.save(permission);
    }

    @Override
    public Permission updatePermission(UpdatePermissionRequestCommand command) throws ServiceException {
        Permission resPermission = getPermissionById(command.permissionId());

        resPermission.setName(command.name());
        resPermission.setApiPath(command.apiPath());
        resPermission.setMethod(command.method());
        resPermission.setModule(command.module());

        return this.permissionRepository.save(resPermission);
    }

    @Override
    public void deletePermission(String permissionId) throws ServiceException {
        Permission permission = getPermissionById(permissionId);
        permissionRepository.delete(permission);
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
