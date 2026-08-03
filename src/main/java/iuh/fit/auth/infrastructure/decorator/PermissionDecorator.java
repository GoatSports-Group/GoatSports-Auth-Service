package iuh.fit.auth.infrastructure.decorator;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.service.PermissionService;
import iuh.fit.auth.application.service.impl.PermissionServiceImpl;
import iuh.fit.auth.domain.entity.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PermissionDecorator implements PermissionService {
    private final PermissionServiceImpl permissionServiceImpl;

    @Override
    @Transactional(readOnly = true)
    public Permission getPermissionById(String permissionId) throws ServiceException {
        return permissionServiceImpl.getPermissionById(permissionId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Permission> getAllPermissions(PageFilter filter) {
        return permissionServiceImpl.getAllPermissions(filter);
    }

    @Override
    public boolean existsPermission(String apiPath, String module, String method) {
        return permissionServiceImpl.existsPermission(apiPath, module, method);
    }
}
