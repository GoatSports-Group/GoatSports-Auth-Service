package iuh.fit.auth.application.port.persistence;

import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.PageFilter;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PermissionRepository {
    Optional<Permission> findById(UUID id);

    boolean existsByApiPathAndModuleAndMethod(String apiPath, String module, String method);

    Set<Permission> findByPermissionIdIn(Set<UUID> permissionIds);

    Permission save(Permission permission) throws ServiceException;

    void delete(Permission permission);

    PageResult<Permission> findAll(PageFilter filter);
}
