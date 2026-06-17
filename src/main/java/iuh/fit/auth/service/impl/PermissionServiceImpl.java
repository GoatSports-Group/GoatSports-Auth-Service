package iuh.fit.auth.service.impl;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import iuh.fit.auth.dto.base.BaseListResponse;
import iuh.fit.auth.entity.Permission;
import iuh.fit.auth.exception.ServiceException;
import iuh.fit.auth.repository.PermissionRepository;
import iuh.fit.auth.service.PermissionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    private final PermissionRepository permissionRepository;

    @Override
    public Permission createPermission(Permission permission) {
        permission.setName(permission.getName().toUpperCase().replace(" ", "_"));
        permission.setMethod(permission.getMethod().toUpperCase());
        permission.setModule(permission.getModule().toUpperCase());
        return permissionRepository.save(permission);
    }

    @Override
    public Permission updatePermission(Permission permission) throws ServiceException {
        Permission resPermission = getPermissionById(permission.getPermissionId().toString());

        resPermission.setName(permission.getName());
        resPermission.setApiPath(permission.getApiPath());
        resPermission.setMethod(permission.getMethod());
        resPermission.setModule(permission.getModule());

        return this.permissionRepository.save(resPermission);
    }

    @Override
    public void deletePermission(String permissionId) throws ServiceException {
        Permission permission = getPermissionById(permissionId);

        if (permission.getRoles() != null) {
            permission.getRoles().forEach(role -> role.getPermissions().remove(permission));
        }

        permissionRepository.delete(permission);
    }

    @Override
    public Permission getPermissionById(String permissionId) throws ServiceException {
        return permissionRepository.findById(UUID.fromString(permissionId))
                .orElseThrow(() -> new ServiceException("Quyền hạn không tồn tại"));
    }

    @Override
    public BaseListResponse getAllPermissions(Specification<Permission> spec, Pageable pageable) {
        Page<Permission> page = permissionRepository.findAll(spec, pageable);

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());

        return new BaseListResponse(meta, page.getContent());
    }

    @Override
    public boolean existsPermission(Permission permission) {
        return permissionRepository.existsByApiPathAndModuleAndMethod(
                permission.getApiPath(), permission.getModule(), permission.getMethod());
    }

}
