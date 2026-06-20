package iuh.fit.auth.application.usecase;

import iuh.fit.auth.application.port.persistence.PermissionRepository;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.permission.CreatePermissionRequestCommand;
import iuh.fit.auth.application.dto.request.permission.UpdatePermissionRequestCommand;

import java.util.UUID;

public class PermissionUseCase {
    private final PermissionRepository permissionRepository;

    public PermissionUseCase(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public Permission createPermission(CreatePermissionRequestCommand command) {
        Permission permission = new Permission();
        permission.setName(command.name().toUpperCase().replace(" ", "_"));
        permission.setApiPath(command.apiPath());
        permission.setMethod(command.method().toUpperCase());
        permission.setModule(command.module().toUpperCase());

        return permissionRepository.save(permission);
    }

    public Permission updatePermission(UpdatePermissionRequestCommand command) throws ServiceException {
        Permission resPermission = getPermissionById(command.permissionId());

        resPermission.setName(command.name());
        resPermission.setApiPath(command.apiPath());
        resPermission.setMethod(command.method());
        resPermission.setModule(command.module());

        return this.permissionRepository.save(resPermission);
    }

    public void deletePermission(String permissionId) throws ServiceException {
        Permission permission = getPermissionById(permissionId);
        permissionRepository.delete(permission);
    }

    public Permission getPermissionById(String permissionId) throws ServiceException {
        return permissionRepository.findById(UUID.fromString(permissionId))
                .orElseThrow(() -> new ServiceException("Quyền hạn không tồn tại"));
    }

    public PageResult<Permission> getAllPermissions(PageFilter filter) {
        return permissionRepository.findAll(filter);
    }

    public boolean existsPermission(String apiPath, String module, String method) {
        return permissionRepository.existsByApiPathAndModuleAndMethod(apiPath, module, method);
    }
}
