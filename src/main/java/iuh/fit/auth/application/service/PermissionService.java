package iuh.fit.auth.application.service;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.Permission;

public interface PermissionService {
    Permission getPermissionById(String permissionId) throws ServiceException;

    PageResult<Permission> getAllPermissions(PageFilter filter);

    boolean existsPermission(String apiPath, String module, String method);
}