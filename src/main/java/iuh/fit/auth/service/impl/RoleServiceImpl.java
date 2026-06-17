package iuh.fit.auth.service.impl;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import iuh.fit.auth.dto.base.BaseListResponse;
import iuh.fit.auth.dto.request.role.RoleCreateRequest;
import iuh.fit.auth.dto.request.role.RoleUpdateRequest;
import iuh.fit.auth.entity.Permission;
import iuh.fit.auth.entity.Role;
import iuh.fit.auth.exception.ServiceException;
import iuh.fit.auth.repository.PermissionRepository;
import iuh.fit.auth.repository.RoleRepository;
import iuh.fit.auth.service.RoleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
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

    @Override
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

    @Override
    public void deleteRole(String roleId) throws ServiceException {
        Role role = getRoleById(roleId);
        roleRepository.delete(role);
    }

    @Override
    public Role activateRole(String roleId) throws ServiceException {
        Role role = getRoleById(roleId);

        if (!role.isActive()) {
            role.setActive(true);
            return roleRepository.save(role);
        }

        return role;
    }

    @Override
    public Role deactivateRole(String roleId) throws ServiceException {
        Role role = getRoleById(roleId);

        if (role.isActive()) {
            role.setActive(false);
            return roleRepository.save(role);
        }

        return role;
    }

    @Override
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

    @Override
    public Role getRoleByName(String name) throws ServiceException {
        return roleRepository.findByName(name).orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
    }

    @Override
    public BaseListResponse getAllRoles(Specification<Role> spec, Pageable pageable) {
        Page<Role> page = roleRepository.findAll(spec, pageable);

        BaseListResponse.Meta meta = new BaseListResponse.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setTotal(page.getTotalElements());
        meta.setPages(page.getTotalPages());

        return new BaseListResponse(meta, page.getContent());
    }
}
