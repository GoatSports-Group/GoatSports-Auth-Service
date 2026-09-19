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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

    private record PermissionSeed(
            String name,
            String apiPath,
            String method,
            String module,
            Set<RoleEnum> roles
    ) {}

    private static final PermissionSeed[] PERMISSION_SEEDS = {
            adminPermission("CREATE_USER", "/api/v1/admin/users", "POST", "USER"),
            adminPermission("GET_USERS", "/api/v1/admin/users", "GET", "USER"),
            adminPermission("ASSIGN_ROLE_USER", "/api/v1/admin/users/role", "PUT", "USER"),
            adminPermission("CHANGE_PASSWORD_USER", "/api/v1/admin/users/{userId}/password", "PUT", "USER"),
            adminPermission("VERIFY_USER", "/api/v1/admin/users/{userId}/verify", "PUT", "USER"),

            adminPermission("CREATE_ROLE", "/api/v1/admin/roles", "POST", "ROLE"),
            adminPermission("UPDATE_ROLE", "/api/v1/admin/roles", "PUT", "ROLE"),
            adminPermission("DELETE_ROLE", "/api/v1/admin/roles/{id}", "DELETE", "ROLE"),
            adminPermission("ACTIVATE_ROLE", "/api/v1/admin/roles/{id}/activate", "PUT", "ROLE"),
            adminPermission("DEACTIVE_ROLE", "/api/v1/admin/roles/{id}/deactivate", "PUT", "ROLE"),
            adminPermission("GET_ROLE", "/api/v1/admin/roles/{id}", "GET", "ROLE"),
            adminPermission("GET_ROLES", "/api/v1/admin/roles", "GET", "ROLE"),

            adminPermission("GET_PERMISSION", "/api/v1/admin/permissions/{id}", "GET", "PERMISSION"),
            adminPermission("GET_PERMISSIONS", "/api/v1/admin/permissions", "GET", "PERMISSION"),
            adminPermission("GET_OWNER_APPLICATIONS", "/api/v1/admin/owner-applications", "GET", "OWNER_APPLICATION"),
            adminPermission("GET_OWNER_APPLICATION", "/api/v1/admin/owner-applications/{id}", "GET", "OWNER_APPLICATION"),
            adminPermission("MARK_OWNER_APPLICATION_VIEWED", "/api/v1/admin/owner-applications/{id}/viewed", "PUT", "OWNER_APPLICATION"),
            adminPermission("APPROVE_OWNER_APPLICATION", "/api/v1/admin/owner-applications/{id}/approve", "POST", "OWNER_APPLICATION"),
            adminPermission("REJECT_OWNER_APPLICATION", "/api/v1/admin/owner-applications/{id}/reject", "POST", "OWNER_APPLICATION"),
            adminPermission("EXPORT_USERS", "/api/v1/reports/export/users", "GET", "REPORT"),
            adminPermission("EXPORT_USER", "/api/v1/reports/export/users/{userId}", "GET", "REPORT"),
            adminPermission("GET_LOGS", "/api/v1/logs", "GET", "AUDIT"),

            adminPermission("GET_ALL_VENUES", "/api/v1/admin/venues", "GET", "VENUE"),
            adminPermission("GET_ANY_VENUE", "/api/v1/admin/venues/{venueId}", "GET", "VENUE"),
            adminPermission("GET_ANY_VENUE_COURTS", "/api/v1/admin/venues/{venueId}/courts", "GET", "VENUE_COURT"),
            adminPermission("GET_ANY_VENUE_COURT", "/api/v1/admin/venue-courts/{venueCourtId}", "GET", "VENUE_COURT"),

            ownerPermission("GET_OWN_VENUES", "/api/v1/owner/venues", "GET", "VENUE"),
            ownerPermission("GET_OWN_VENUE", "/api/v1/owner/venues/{venueId}", "GET", "VENUE"),
            ownerPermission("UPDATE_OWN_VENUE", "/api/v1/owner/venues/{venueId}", "PUT", "VENUE"),
            ownerPermission("GET_OWN_VENUE_COURTS", "/api/v1/owner/venues/{venueId}/courts", "GET", "VENUE_COURT"),
            ownerPermission("CREATE_OWN_VENUE_COURT", "/api/v1/owner/venues/{venueId}/courts", "POST", "VENUE_COURT"),
            ownerPermission("GET_OWN_VENUE_COURT", "/api/v1/owner/venue-courts/{venueCourtId}", "GET", "VENUE_COURT"),
            ownerPermission("UPDATE_OWN_VENUE_COURT", "/api/v1/owner/venue-courts/{venueCourtId}", "PUT", "VENUE_COURT"),
            ownerPermission("CHANGE_OWN_VENUE_COURT_STATUS", "/api/v1/owner/venue-courts/{venueCourtId}", "PATCH", "VENUE_COURT"),
            ownerPermission("DELETE_OWN_VENUE_COURT", "/api/v1/owner/venue-courts/{venueCourtId}", "DELETE", "VENUE_COURT"),

            ownerPermission("GET_OWN_COURT_PRICING_RULES", "/api/v1/owner/venue-courts/{venueCourtId}/pricing-rules", "GET", "COURT_PRICING_RULE"),
            ownerPermission("CREATE_OWN_COURT_PRICING_RULE", "/api/v1/owner/venue-courts/{venueCourtId}/pricing-rules", "POST", "COURT_PRICING_RULE"),
            ownerPermission("UPDATE_OWN_COURT_PRICING_RULE", "/api/v1/owner/venue-courts/{venueCourtId}/pricing-rules/{pricingRuleId}", "PUT", "COURT_PRICING_RULE"),
            ownerPermission("DELETE_OWN_COURT_PRICING_RULE", "/api/v1/owner/venue-courts/{venueCourtId}/pricing-rules/{pricingRuleId}", "DELETE", "COURT_PRICING_RULE"),
            ownerPermission("GET_OWN_TIME_SLOTS", "/api/v1/owner/venue-courts/{venueCourtId}/time-slots", "GET", "TIME_SLOT"),
            ownerPermission("CREATE_OWN_TIME_SLOTS", "/api/v1/owner/venue-courts/{venueCourtId}/time-slots", "POST", "TIME_SLOT"),
            ownerPermission("UPDATE_OWN_TIME_SLOT_STATUS", "/api/v1/owner/time-slots/{timeSlotId}", "PATCH", "TIME_SLOT"),
            ownerPermission("DELETE_OWN_TIME_SLOT", "/api/v1/owner/time-slots/{timeSlotId}", "DELETE", "TIME_SLOT"),

            ownerPermission("CREATE_OWN_OWNER_APPLICATION", "/api/v1/owner-applications", "POST", "OWNER_APPLICATION"),
            ownerPermission("GET_OWN_OWNER_APPLICATION", "/api/v1/owner-applications/me", "GET", "OWNER_APPLICATION"),

            ownerPermission("GET_OWN_BOOKINGS", "/api/v1/owner/bookings", "GET", "BOOKING"),
            ownerPermission("GET_OWN_BOOKING", "/api/v1/owner/bookings/{bookingId}", "GET", "BOOKING"),
            ownerPermission("UPDATE_OWN_BOOKING_STATUS", "/api/v1/owner/bookings/{bookingId}/status", "PATCH", "BOOKING"),

            ownerPermission("LOOKUP_OWN_CHECK_IN", "/api/v1/owner/check-ins/lookup", "POST", "CHECK_IN"),
            ownerPermission("GET_OWN_CHECK_INS", "/api/v1/owner/check-ins", "GET", "CHECK_IN"),
            ownerPermission("CONFIRM_OWN_CHECK_IN", "/api/v1/owner/check-ins", "POST", "CHECK_IN"),
            ownerPermission("CREATE_OWN_WALK_IN_BOOKING", "/api/v1/owner/check-ins/walk-ins", "POST", "CHECK_IN"),

            ownerPermission("GET_OWN_REVENUE", "/api/v1/owner/revenue", "GET", "REVENUE"),

            ownerPermission("GET_OWN_REVIEWS", "/api/v1/owner/reviews", "GET", "REVIEW"),
            playerPermission("CREATE_OWN_REVIEW", "/api/v1/player/reviews", "POST", "REVIEW")
    };

    private static PermissionSeed adminPermission(String name, String path, String method, String module) {
        return new PermissionSeed(name, path, method, module, Set.of(RoleEnum.ADMIN));
    }

    private static PermissionSeed ownerPermission(String name, String path, String method, String module) {
        return new PermissionSeed(name, path, method, module, Set.of(RoleEnum.VENUE_OWNER));
    }

    private static PermissionSeed playerPermission(String name, String path, String method, String module) {
        return new PermissionSeed(name, path, method, module, Set.of(RoleEnum.PLAYER));
    }

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
        Map<String, PermissionEntity> managedPermissions = new HashMap<>();
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
            if (!Objects.equals(permission.getApiPath(), seed.apiPath())
                    || !Objects.equals(permission.getMethod(), seed.method())
                    || !Objects.equals(permission.getModule(), seed.module())) {
                permission.setApiPath(seed.apiPath());
                permission.setMethod(seed.method());
                permission.setModule(seed.module());
                permission = permissionRepository.save(permission);
            }
            managedPermissions.put(seed.name(), permission);
        }

        for (RoleEnum roleName : RoleEnum.values()) {
            roleRepository.findByName(roleName).ifPresent(role -> {
                boolean updated = false;
                if (role.getPermissions() == null) {
                    role.setPermissions(new HashSet<>());
                }
                for (PermissionSeed seed : PERMISSION_SEEDS) {
                    PermissionEntity permission = managedPermissions.get(seed.name());
                    boolean assigned = role.getPermissions().stream()
                            .anyMatch(item -> item.getName().equals(seed.name()));
                    boolean shouldBeAssigned = seed.roles().contains(roleName);
                    if (shouldBeAssigned && !assigned) {
                        role.getPermissions().add(permission);
                        updated = true;
                    } else if (!shouldBeAssigned && assigned) {
                        role.getPermissions().removeIf(item -> item.getName().equals(seed.name()));
                        updated = true;
                    }
                }
                if (updated) {
                    roleRepository.save(role);
                    log.info("Đã đồng bộ quyền hạn cho vai trò {}", roleName);
                }
            });
        }
    }
}
