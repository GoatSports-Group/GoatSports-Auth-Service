package iuh.fit.auth.infrastructure.config;

import iuh.fit.auth.application.port.cache.RedisPort;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.application.port.security.JwtPort;
import iuh.fit.auth.application.port.security.KeycloakPort;
import iuh.fit.auth.application.usecase.auth.AuthenticationUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public AuthenticationUseCase authenticationUseCase(
            JwtPort jwtPort, RedisPort redisPort, KeycloakPort keycloakPort,
            EmailNotificationPort emailNotificationPort, CurrentUserProvider currentUserProvider,
            UserRepository userRepository, RoleRepository roleRepository
    ) {
        return new AuthenticationUseCase(
                jwtPort, redisPort, keycloakPort,
                emailNotificationPort, currentUserProvider,
                userRepository, roleRepository
        );
    }

}
