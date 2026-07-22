package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.request.permission.CreatePermissionRequestCommand;
import iuh.fit.auth.application.dto.request.permission.UpdatePermissionRequestCommand;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.Permission;

public interface PermissionService {
    Permission createPermission(CreatePermissionRequestCommand command) throws ServiceException;

    Permission updatePermission(UpdatePermissionRequestCommand command) throws ServiceException;

    void deletePermission(String permissionId) throws ServiceException;

    Permission getPermissionById(String permissionId) throws ServiceException;

    PageResult<Permission> getAllPermissions(PageFilter filter);

    boolean existsPermission(String apiPath, String module, String method);
}