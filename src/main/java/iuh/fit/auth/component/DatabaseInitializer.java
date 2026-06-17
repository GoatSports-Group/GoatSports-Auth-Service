package iuh.fit.auth.component;

import java.util.UUID;

import iuh.fit.auth.enums.AuthProvider;
import org.hibernate.service.spi.ServiceException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import iuh.fit.auth.entity.Role;
import iuh.fit.auth.entity.User;
import iuh.fit.auth.enums.RoleEnum;
import iuh.fit.auth.enums.UserStatusEnum;
import iuh.fit.auth.repository.RoleRepository;
import iuh.fit.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
        initializeAdmin();
    }

    private void initializeRoles() {
        for (RoleEnum roleEnum : RoleEnum.values()) {
            String roleName = roleEnum.name();
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = Role.builder()
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
             User user = new User();
             user.setUsername("admin");
             user.setPassword(passwordEncoder.encode("Admin12345678@"));
             user.setFullName("Admin");
             user.setEmail("admin@gmail.com");
             user.setStatus(UserStatusEnum.ACTIVE.name());
             user.setAuthProvider(AuthProvider.LOCAL);

             Role role = roleRepository.findByName(RoleEnum.ADMIN.name())
                     .orElseThrow(() -> new ServiceException("Vai trò không tồn tại"));
             user.setRole(role);

             userRepository.save(user);
             log.info("Đã khởi tạo admin");
         }

     }
}
