package iuh.fit.auth.infrastructure;

import iuh.fit.auth.domain.enums.AuthProviderEnum;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import iuh.fit.auth.infrastructure.persistence.entity.PermissionEntity;
import iuh.fit.auth.infrastructure.persistence.entity.RoleEntity;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaPermissionRepository;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaRoleRepository;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DatabaseInitializer implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;
    private final JpaRoleRepository roleRepository;
    private final JpaUserRepository userRepository;
    private final JpaPermissionRepository permissionRepository;

    private record PermissionSeed(String name, String apiPath, String method, String module) {}

    private static final PermissionSeed[] PERMISSION_SEEDS = {
            new PermissionSeed("CREATE_USER", "/api/v1/admin/users", "POST", "USER"),
            new PermissionSeed("GET_USERS", "/api/v1/admin/users", "GET", "USER"),
            new PermissionSeed("ASSIGN_ROLE_USER", "/api/v1/admin/users/role", "PUT", "USER"),
            new PermissionSeed("CHANGE_PASSWORD_USER", "/api/v1/admin/users/{userId}/password", "PUT", "USER"),
            new PermissionSeed("VERIFY_USER", "/api/v1/admin/users/{userId}/verify", "PUT", "USER"),

            new PermissionSeed("CREATE_ROLE", "/api/v1/admin/roles", "POST", "ROLE"),
            new PermissionSeed("UPDATE_ROLE", "/api/v1/admin/roles", "PUT", "ROLE"),
            new PermissionSeed("DELETE_ROLE", "/api/v1/admin/roles/{id}", "DELETE", "ROLE"),
            new PermissionSeed("ACTIVATE_ROLE", "/api/v1/admin/roles/{id}/activate", "PUT", "ROLE"),
            new PermissionSeed("DEACTIVE_ROLE", "/api/v1/admin/roles/{id}/deactivate", "PUT", "ROLE"),
            new PermissionSeed("GET_ROLE", "/api/v1/admin/roles/{id}", "GET", "ROLE"),
            new PermissionSeed("GET_ROLES", "/api/v1/admin/roles", "GET", "ROLE"),

            new PermissionSeed("GET_PERMISSION", "/api/v1/admin/permissions/{id}", "GET", "PERMISSION"),
            new PermissionSeed("GET_PERMISSIONS", "/api/v1/admin/permissions", "GET", "PERMISSION"),

            new PermissionSeed("GET_OWNER_APPLICATIONS", "/api/v1/admin/owner-applications", "GET", "OWNER_APPLICATION"),
            new PermissionSeed("GET_OWNER_APPLICATION", "/api/v1/admin/owner-applications/{id}", "GET", "OWNER_APPLICATION"),

            new PermissionSeed("EXPORT_USERS", "/api/v1/reports/export/users", "GET", "REPORT"),
            new PermissionSeed("EXPORT_USER", "/api/v1/reports/export/users/{userId}", "GET", "REPORT"),

            new PermissionSeed("GET_LOGS", "/api/v1/logs", "GET", "AUDIT")
    };

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeAdmin();
        initializePermissions();
    }

    private void initializeRoles() {
        for (RoleEnum roleEnum : RoleEnum.values()) {
            String roleName = roleEnum.name();
            if (roleRepository.findByName(roleEnum).isEmpty()) {
                RoleEntity role = RoleEntity.builder()
                        .name(roleEnum)
                        .description("Vai trò " + roleEnum.getValue().toLowerCase())
                        .active(true)
                        .build();
                roleRepository.save(role);
                log.info("Đã khởi tạo vai trò: {}", roleName);
            }
        }
    }

    private void initializeAdmin() {
        boolean existed = userRepository.existsByUsernameOrEmail("admin", "admin@gmail.com");

        if (!existed) {
            UserEntity user = new UserEntity();
            user.setUsername("admin");
            user.setPassword(passwordEncoder.encode("Admin12345678@"));
            user.setFullName("Admin");
            user.setEmail("admin@gmail.com");
            user.setStatus(UserStatusEnum.ACTIVE);
            user.getAuthProviders().add(AuthProviderEnum.LOCAL);

            RoleEntity role = roleRepository.findByName(RoleEnum.ADMIN)
                    .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
            user.setRole(role);

            userRepository.save(user);
            log.info("Đã khởi tạo admin");
        }
    }

    private void initializePermissions() {
        Set<PermissionEntity> adminPermissions = new HashSet<>();
        for (PermissionSeed seed : PERMISSION_SEEDS) {
            PermissionEntity permission = permissionRepository.findByName(seed.name())
                    .orElseGet(() -> {
                        PermissionEntity newPerm = PermissionEntity.builder()
                                .name(seed.name())
                                .apiPath(seed.apiPath())
                                .method(seed.method())
                                .module(seed.module())
                                .build();
                        PermissionEntity saved = permissionRepository.save(newPerm);
                        log.info("Đã khởi tạo quyền hạn: {}", seed.name());
                        return saved;
                    });
            adminPermissions.add(permission);
        }

        roleRepository.findByName(RoleEnum.ADMIN).ifPresent(adminRole -> {
            boolean updated = false;
            if (adminRole.getPermissions() == null) {
                adminRole.setPermissions(new HashSet<>());
            }
            for (PermissionEntity perm : adminPermissions) {
                if (adminRole.getPermissions().stream().noneMatch(p -> p.getName().equals(perm.getName()))) {
                    adminRole.getPermissions().add(perm);
                    updated = true;
                }
            }
            if (updated) {
                roleRepository.save(adminRole);
                log.info("Đã gán toàn bộ quyền hạn cho vai trò ADMIN");
            }
        });
    }
}
