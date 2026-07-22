package iuh.fit.auth.infrastructure.config;

import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.port.cache.RedisPort;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.port.persistence.PermissionRepository;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.application.port.security.JwtPort;
import iuh.fit.auth.application.port.event.UserRegistrationEventPublisher;
import iuh.fit.auth.application.port.security.KeycloakPort;
import iuh.fit.auth.application.service.impl.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationServiceConfig {

    @Bean
    public AuthenticationServiceImpl authenticationService(
            JwtPort jwtPort, RedisPort redisPort, KeycloakPort keycloakPort,
            EmailNotificationPort emailNotificationPort, CurrentUserProvider currentUserProvider,
            UserRegistrationEventPublisher userRegistrationEventPublisher,
            UserRepository userRepository, RoleRepository roleRepository,
            UserMapper userMapper
    ) {
        return new AuthenticationServiceImpl(
                jwtPort, redisPort, keycloakPort,
                emailNotificationPort, currentUserProvider,
                userRegistrationEventPublisher,
                userRepository, roleRepository,
                userMapper
        );
    }

    @Bean
    public UserServiceImpl userService(
            JwtPort jwtPort, UserRepository userRepository, RoleRepository roleRepository,
            UserMapper userMapper
    ) {
        return new UserServiceImpl(jwtPort, userRepository, roleRepository, userMapper);
    }

    @Bean
    public RoleServiceImpl roleService(
            RoleRepository roleRepository, PermissionRepository permissionRepository
    ) {
        return new RoleServiceImpl(roleRepository, permissionRepository);
    }

    @Bean
    public PermissionServiceImpl permissionService(PermissionRepository permissionRepository) {
        return new PermissionServiceImpl(permissionRepository);
    }

    @Bean
    public InternalServiceImpl internalService(
            UserRepository userRepository, RoleRepository roleRepository,
            UserMapper userMapper
    ) {
        return new InternalServiceImpl(userRepository, roleRepository, userMapper);
    }

}
