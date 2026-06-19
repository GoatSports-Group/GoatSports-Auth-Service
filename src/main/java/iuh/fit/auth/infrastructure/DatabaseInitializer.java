package iuh.fit.auth.infrastructure;

import iuh.fit.auth.domain.enums.AuthProviderEnum;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import iuh.fit.auth.infrastructure.persistence.entity.RoleEntity;
import iuh.fit.auth.infrastructure.persistence.entity.UserEntity;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaRoleRepository;
import iuh.fit.auth.infrastructure.persistence.repository.jpa.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.service.spi.ServiceException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;
    private final JpaRoleRepository roleRepository;
    private final JpaUserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeAdmin();
    }

    private void initializeRoles() {
        for (RoleEnum roleEnum : RoleEnum.values()) {
            String roleName = roleEnum.name();
            if (roleRepository.findByName(roleName).isEmpty()) {
                RoleEntity role = RoleEntity.builder()
                        .name(roleName)
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
            user.setAuthProvider(AuthProviderEnum.LOCAL);

            RoleEntity role = roleRepository.findByName(RoleEnum.ADMIN.name())
                    .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
            user.setRole(role);

            userRepository.save(user);
            log.info("Đã khởi tạo admin");
        }
    }
}
