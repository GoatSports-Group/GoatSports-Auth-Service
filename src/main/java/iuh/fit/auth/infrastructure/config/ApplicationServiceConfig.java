package iuh.fit.auth.infrastructure.config;

import iuh.fit.auth.application.service.impl.AdminStatsServiceImpl;
import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.port.cache.RedisPort;
import iuh.fit.auth.application.port.client.StoragePort;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.port.persistence.PermissionRepository;
import iuh.fit.auth.application.port.persistence.PlayerSportProfileRepository;
import iuh.fit.auth.application.port.persistence.AppliedMatchResultRepository;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.application.port.security.JwtPort;
import iuh.fit.auth.application.port.event.UserRegistrationEventPublisher;
import iuh.fit.auth.application.port.security.KeycloakPort;
import iuh.fit.auth.application.port.security.RsaPort;
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
            UserMapper userMapper, RsaPort rsaPort
    ) {
        return new AuthenticationServiceImpl(
                jwtPort, redisPort, keycloakPort,
                emailNotificationPort, currentUserProvider,
                userRegistrationEventPublisher,
                userRepository, roleRepository,
                userMapper, rsaPort
        );
    }

    @Bean
    public UserServiceImpl userService(
            JwtPort jwtPort, UserRepository userRepository, RoleRepository roleRepository,
            UserMapper userMapper, UserRegistrationEventPublisher userRegistrationEventPublisher,
            StoragePort storagePort, CurrentUserProvider currentUserProvider,
            EmailNotificationPort emailNotificationPort, RsaPort rsaPort
    ) {
        return new UserServiceImpl(
                jwtPort, userRepository, roleRepository,
                userMapper, userRegistrationEventPublisher, storagePort,
                currentUserProvider, emailNotificationPort, rsaPort
        );
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
    public PlayerSportProfileServiceImpl playerSportProfileService(
            PlayerSportProfileRepository profileRepository,
            CurrentUserProvider currentUserProvider
    ) {
        return new PlayerSportProfileServiceImpl(profileRepository, currentUserProvider);
    }

    @Bean
    public AdminStatsServiceImpl adminStatsService(UserRepository userRepository) {
        return new AdminStatsServiceImpl(userRepository);
    }

    @Bean
    public MatchResultServiceImpl matchResultService(
            PlayerSportProfileRepository profileRepository,
            AppliedMatchResultRepository resultRepository,
            CurrentUserProvider currentUserProvider
    ) {
        return new MatchResultServiceImpl(profileRepository, resultRepository, currentUserProvider);
    }

    @Bean
    public InternalServiceImpl internalService(
            UserRepository userRepository, RoleRepository roleRepository,
            UserMapper userMapper
    ) {
        return new InternalServiceImpl(userRepository, roleRepository, userMapper);
    }

}
