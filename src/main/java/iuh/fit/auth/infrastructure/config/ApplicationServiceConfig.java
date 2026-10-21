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
            UserMapper userMapper, RsaPort rsaPort,
            iuh.fit.auth.application.service.AccountAppealService accountAppealService,
            iuh.fit.auth.application.port.client.SessionRevocationPort sessionRevocationPort
    ) {
        return new AuthenticationServiceImpl(
                jwtPort, redisPort, keycloakPort,
                emailNotificationPort, currentUserProvider,
                userRegistrationEventPublisher,
                userRepository, roleRepository,
                userMapper, rsaPort, accountAppealService, sessionRevocationPort
        );
    }

    @Bean
    public UserServiceImpl userService(
            JwtPort jwtPort, UserRepository userRepository, RoleRepository roleRepository,
            UserMapper userMapper, UserRegistrationEventPublisher userRegistrationEventPublisher,
            StoragePort storagePort, CurrentUserProvider currentUserProvider,
            EmailNotificationPort emailNotificationPort, RsaPort rsaPort, RedisPort redisPort,
            iuh.fit.auth.application.port.client.SessionRevocationPort sessionRevocationPort
    ) {
        return new UserServiceImpl(
                jwtPort, userRepository, roleRepository,
                userMapper, userRegistrationEventPublisher, storagePort,
                currentUserProvider, emailNotificationPort, rsaPort, redisPort, sessionRevocationPort
        );
    }

    @Bean
    public iuh.fit.auth.application.service.impl.AccountDeletionServiceImpl accountDeletionServiceImpl(
            UserRepository userRepository, PlayerSportProfileRepository profileRepository,
            iuh.fit.auth.application.port.persistence.AccountAppealRepository appealRepository,
            RedisPort redisPort, KeycloakPort keycloakPort,
            EmailNotificationPort emailNotificationPort, UserRegistrationEventPublisher eventPublisher,
            iuh.fit.auth.application.port.client.AccountDeletionBlockerPort blockerPort,
            CurrentUserProvider currentUserProvider, JwtPort jwtPort,
            iuh.fit.auth.application.port.client.SessionRevocationPort sessionRevocationPort
    ) {
        return new iuh.fit.auth.application.service.impl.AccountDeletionServiceImpl(
                userRepository, profileRepository, appealRepository, redisPort, keycloakPort,
                emailNotificationPort, eventPublisher, blockerPort, currentUserProvider, jwtPort, sessionRevocationPort,
                java.time.Clock.systemDefaultZone());
    }

    @Bean
    public RoleServiceImpl roleService(
            RoleRepository roleRepository, PermissionRepository permissionRepository,
            UserRepository userRepository, RedisPort redisPort
    ) {
        return new RoleServiceImpl(roleRepository, permissionRepository, userRepository, redisPort);
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

    @Bean
    public iuh.fit.auth.application.service.impl.AccountAppealServiceImpl accountAppealService(
            iuh.fit.auth.application.port.persistence.AccountAppealRepository appealRepository,
            UserRepository userRepository, RedisPort redisPort, EmailNotificationPort emailNotificationPort,
            CurrentUserProvider currentUserProvider
    ) {
        return new iuh.fit.auth.application.service.impl.AccountAppealServiceImpl(
                appealRepository, userRepository, redisPort, emailNotificationPort, currentUserProvider,
                java.time.Clock.systemDefaultZone());
    }
}
