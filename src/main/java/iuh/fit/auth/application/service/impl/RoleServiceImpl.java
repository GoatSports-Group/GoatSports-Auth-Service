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
import iuh.fit.auth.application.port.persistence.UserRepository;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Vai trò: ba vai trò hệ thống ({@link RoleEnum}: code dựa vào tên của chúng) và vai trò admin tự tạo (ví dụ
 * SUPPORT_STAFF). Quyền thật nằm ở danh sách permission của vai trò, nên vai trò tự tạo dùng được như vai trò hệ thống.
 */
public class RoleServiceImpl implements RoleService {
    private static final Pattern ROLE_NAME = Pattern.compile("^[A-Z][A-Z0-9_]{1,49}$");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final RedisPort redisPort;

    public RoleServiceImpl(
            RoleRepository roleRepository, PermissionRepository permissionRepository,
            UserRepository userRepository, RedisPort redisPort
    ) {
        this.redisPort = redisPort;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Role createRole(CreateRoleRequestCommand command) throws ServiceException {
        String name = normalizeName(command.name());
        ensureNameFree(name, null);
        Role role = new Role();
        role.setName(name);
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

        String previousName = resRole.getName();
        String name = normalizeName(command.name());
        if (isSystem(previousName) && !previousName.equals(name)) {
            throw new ServiceException("Không đổi được tên vai trò hệ thống " + previousName + ".");
        }
        ensureNameFree(name, resRole.getRoleId());
        if (RoleEnum.ADMIN.name().equals(previousName) && !command.active()) {
            throw new ServiceException("Không tạm ngưng được vai trò ADMIN.");
        }
        resRole.setDescription(command.description());
        resRole.setActive(command.active());
        resRole.setName(name);

        Role saved = roleRepository.save(resRole);
        redisPort.evictRolePermissions(previousName);
        redisPort.evictRolePermissions(saved.getName());
        return saved;
    }

    @Override
    public void deleteRole(String roleId) throws ServiceException {
        Role role = getRoleById(roleId);
        if (isSystem(role.getName())) {
            throw new ServiceException("Không xóa được vai trò hệ thống " + role.getName() + ".");
        }
        long holders = userRepository.countByRole().getOrDefault(role.getName(), 0L);
        if (holders > 0) {
            throw new ServiceException("Còn " + holders + " tài khoản mang vai trò " + role.getName()
                    + ". Đổi vai trò cho các tài khoản này trước khi xóa.");
        }
        roleRepository.delete(role);
        redisPort.evictRolePermissions(role.getName());
    }

    @Override
    public Role activateRole(String roleId) throws ServiceException {
        Role role = getRoleById(roleId);

        if (!role.isActive()) {
            role.setActive(true);
            role = roleRepository.save(role);
            redisPort.evictRolePermissions(role.getName());
        }

        return role;
    }

    @Override
    public Role deactivateRole(String roleId) throws ServiceException {
        Role role = getRoleById(roleId);
        if (RoleEnum.ADMIN.name().equals(role.getName())) {
            throw new ServiceException("Không tạm ngưng được vai trò ADMIN.");
        }

        if (role.isActive()) {
            role.setActive(false);
            role = roleRepository.save(role);
            redisPort.evictRolePermissions(role.getName());
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

    /** Chữ in hoa, khoảng trắng thành "_" (giống form admin), rồi kiểm định dạng. */
    static String normalizeName(String raw) throws ServiceException {
        String name = raw == null ? "" : raw.trim().toUpperCase().replaceAll("\\s+", "_");
        if (!ROLE_NAME.matcher(name).matches()) {
            throw new ServiceException(
                    "Tên vai trò chỉ gồm chữ in hoa không dấu, số và dấu gạch dưới, bắt đầu bằng chữ, 2–50 ký tự (vd SUPPORT_STAFF).");
        }
        return name;
    }

    private void ensureNameFree(String name, UUID ownId) throws ServiceException {
        Optional<Role> existing = roleRepository.findByName(name);
        if (existing.isPresent() && !existing.get().getRoleId().equals(ownId)) {
            throw new ServiceException("Vai trò " + name + " đã tồn tại.");
        }
    }

    private static boolean isSystem(String name) {
        for (RoleEnum role : RoleEnum.values()) {
            if (role.name().equals(name)) return true;
        }
        return false;
    }
}
