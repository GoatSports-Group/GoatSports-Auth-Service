package iuh.fit.auth.application.usecase.permission;

import iuh.fit.auth.application.port.persistence.PermissionRepository;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.infrastructure.persistence.entity.PermissionEntity;
import iuh.fit.auth.presentation.response.base.BaseListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PermissionUseCase {
    private final PermissionRepository permissionRepository;

    public Permission createPermission(Permission permission) {
        permission.setName(permission.getName().toUpperCase().replace(" ", "_"));
        permission.setMethod(permission.getMethod().toUpperCase());
        permission.setModule(permission.getModule().toUpperCase());
        return permissionRepository.save(permission);
    }

    public Permission updatePermission(Permission permission) throws ServiceException {
        Permission resPermission = getPermissionById(permission.getPermissionId().toString());

        resPermission.setName(permission.getName());
        resPermission.setApiPath(permission.getApiPath());
        resPermission.setMethod(permission.getMethod());
        resPermission.setModule(permission.getModule());

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

    public BaseListResponse getAllPermissions(Specification<PermissionEntity> spec, Pageable pageable) {
        Page<Permission> page = permissionRepository.findAll(spec, pageable);

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());

        return new BaseListResponse(meta, page.getContent());
    }

    public boolean existsPermission(Permission permission) {
        return permissionRepository.existsByApiPathAndModuleAndMethod(
                permission.getApiPath(), permission.getModule(), permission.getMethod());
    }
}
