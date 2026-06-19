package iuh.fit.auth.application.usecase.role;

import iuh.fit.auth.application.port.persistence.PermissionRepository;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.infrastructure.persistence.entity.RoleEntity;
import iuh.fit.auth.presentation.request.role.RoleCreateRequest;
import iuh.fit.auth.presentation.request.role.RoleUpdateRequest;
import iuh.fit.auth.presentation.response.base.BaseListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleUseCase {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public Role createRole(RoleCreateRequest request) {
        Role role = new Role();
        role.setName(request.getName().toUpperCase());
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        } else {
            role.setDescription("");
        }
        role.setActive(true);

        return roleRepository.save(role);
    }

    public Role updateRole(RoleUpdateRequest request) throws ServiceException {
        Role resRole = getRoleById(request.getRoleId());

        if (request.getPermissionIds() != null) {
            Set<UUID> permissionIds = request.getPermissionIds().stream().map(UUID::fromString)
                    .collect(Collectors.toSet());
            Set<Permission> permissions = permissionRepository.findByPermissionIdIn(permissionIds);
            resRole.setPermissions(permissions);
        }

        resRole.setDescription(request.getDescription());
        resRole.setActive(request.isActive());
        resRole.setName(request.getName());

        return roleRepository.save(resRole);
    }

    public void deleteRole(String roleId) throws ServiceException {
        Role role = getRoleById(roleId);
        roleRepository.delete(role);
    }

    public Role activateRole(String roleId) throws ServiceException {
        Role role = getRoleById(roleId);

        if (!role.isActive()) {
            role.setActive(true);
            return roleRepository.save(role);
        }

        return role;
    }

    public Role deactivateRole(String roleId) throws ServiceException {
        Role role = getRoleById(roleId);

        if (role.isActive()) {
            role.setActive(false);
            return roleRepository.save(role);
        }

        return role;
    }

    public Role getRoleById(String roleId) throws ServiceException {
        UUID uuid;
        try {
            uuid = UUID.fromString(roleId);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ServiceException("Id không hợp lệ: không phải UUID");
        }

        return roleRepository.findById(uuid)
                .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
    }

    public Role getRoleByName(String name) throws ServiceException {
        return roleRepository.findByName(name).orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
    }

    public BaseListResponse getAllRoles(Specification<RoleEntity> spec, Pageable pageable) {
        Page<Role> page = roleRepository.findAll(spec, pageable);

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());

        return new BaseListResponse(meta, page.getContent());
    }
}
