package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.PageFilter;
import iuh.fit.auth.application.port.cache.RedisPort;
import iuh.fit.auth.application.port.persistence.PermissionRepository;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.service.RoleService;
import iuh.fit.auth.application.util.BaseUtil;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.dto.request.role.CreateRoleRequestCommand;
import iuh.fit.auth.application.dto.request.role.UpdateRoleRequestCommand;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RedisPort redisPort;

    public RoleServiceImpl(RoleRepository roleRepository, PermissionRepository permissionRepository, RedisPort redisPort) {
        this.redisPort = redisPort;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public Role createRole(CreateRoleRequestCommand command) throws ServiceException {
        Role role = new Role();
        role.setName(parseRole(command.name()));
        if (command.description() != null) {
            role.setDescription(command.description());
        } else {
            role.setDescription("");
        }
        role.setActive(true);

        return roleRepository.save(role);
    }

    @Override
    public Role updateRole(UpdateRoleRequestCommand command) throws ServiceException {
        Role resRole = getRoleById(command.roleId());

        if (command.permissionIds() != null) {
            Set<UUID> permissionIds = command.permissionIds().stream()
                    .map(UUID::fromString)
                    .collect(Collectors.toSet());
            Set<Permission> permissions = permissionRepository.findByPermissionIdIn(permissionIds);
            resRole.setPermissions(permissions);
        }

        RoleEnum previousName = resRole.getName();
        resRole.setDescription(command.description());
        resRole.setActive(command.active());
        resRole.setName(parseRole(command.name()));

        Role saved = roleRepository.save(resRole);
        redisPort.evictRolePermissions(previousName.name());
        redisPort.evictRolePermissions(saved.getName().name());
        return saved;
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
            role = roleRepository.save(role);
        }

        return role;
    }

    @Override
    public Role deactivateRole(String roleId) throws ServiceException {
        Role role = getRoleById(roleId);

        if (role.isActive()) {
            role.setActive(false);
            role = roleRepository.save(role);
        }

        return role;
    }

    @Override
    public Role getRoleById(String roleId) throws ServiceException {
        UUID uuid = BaseUtil.uuid(roleId);

        return roleRepository.findById(uuid)
                .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
    }

    @Override
    public Role getRoleByName(String name) throws ServiceException {
       return roleRepository.findByName(name)
                .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
    }

    @Override
    public PageResult<Role> getAllRoles(PageFilter filter) {
        return roleRepository.findAll(filter);
    }

    private RoleEnum parseRole(String name) throws ServiceException {
        try {
            return RoleEnum.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ServiceException("Vai trò không hợp lệ: " + name);
        }
    }
}
