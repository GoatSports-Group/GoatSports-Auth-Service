package iuh.fit.auth.application.usecase;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.port.persistence.PermissionRepository;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.request.role.CreateRoleRequestCommand;
import iuh.fit.auth.application.dto.request.role.UpdateRoleRequestCommand;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RoleUseCase {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleUseCase(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    public Role createRole(CreateRoleRequestCommand command) {
        Role role = new Role();
        role.setName(command.name().toUpperCase());
        if (command.description() != null) {
            role.setDescription(command.description());
        } else {
            role.setDescription("");
        }
        role.setActive(true);

        return roleRepository.save(role);
    }

    public Role updateRole(UpdateRoleRequestCommand command) throws ServiceException {
        Role resRole = getRoleById(command.roleId());

        if (command.permissionIds() != null) {
            Set<UUID> permissionIds = command.permissionIds().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());
            Set<Permission> permissions = permissionRepository.findByPermissionIdIn(permissionIds);
            resRole.setPermissions(permissions);
        }

        resRole.setDescription(command.description());
        resRole.setActive(command.active());
        resRole.setName(command.name());

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
            role = roleRepository.save(role);
        }

        return role;
    }

    public Role deactivateRole(String roleId) throws ServiceException {
        Role role = getRoleById(roleId);

        if (role.isActive()) {
            role.setActive(false);
            role = roleRepository.save(role);
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
       return roleRepository.findByName(name)
                .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
    }

    public PageResult<Role> getAllRoles(PageFilter filter) {
        return roleRepository.findAll(filter);
    }
}
