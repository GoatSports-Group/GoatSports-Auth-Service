package iuh.fit.auth.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import iuh.fit.auth.dto.base.BaseListResponse;
import iuh.fit.auth.entity.Permission;
import iuh.fit.auth.exception.ServiceException;

public interface PermissionService {
    Permission createPermission(Permission permission);

    Permission updatePermission(Permission permission) throws ServiceException;

    void deletePermission(String permissionId) throws ServiceException;

    Permission getPermissionById(String permissionId) throws ServiceException;

    BaseListResponse getAllPermissions(Specification<Permission> spec, Pageable pageable);

    boolean existsPermission(Permission permission);
}
