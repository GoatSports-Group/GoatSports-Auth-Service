package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.request.auth.RegisterRequestCommand;
import iuh.fit.auth.application.dto.AuthProperties;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.port.cache.RedisPort;
import iuh.fit.auth.application.port.event.UserRegistrationEventPublisher;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.application.port.security.JwtPort;
import iuh.fit.auth.application.port.security.KeycloakPort;
import iuh.fit.auth.application.port.security.RsaPort;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.RoleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private JwtPort jwtPort;
    @Mock
    private RedisPort redisPort;
    @Mock
    private KeycloakPort keycloakPort;
    @Mock
    private EmailNotificationPort emailNotificationPort;
    @Mock
    private CurrentUserProvider currentUserProvider;
    @Mock
    private UserRegistrationEventPublisher userRegistrationEventPublisher;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RsaPort rsaPort;

    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImpl(
                jwtPort,
                redisPort,
                keycloakPort,
                emailNotificationPort,
                currentUserProvider,
                userRegistrationEventPublisher,
                userRepository,
                roleRepository,
                userMapper,
                rsaPort
        );
    }

    @Test
    void registerChecksDuplicateAgainstUsernameAndEmail() {
        RegisterRequestCommand command = new RegisterRequestCommand(
                "goat.player",
                "Nguyễn Minh",
                "player@example.com",
                "encrypted-password",
                "encrypted-confirm-password"
        );
        when(userRepository.existsByUsernameOrEmail(command.username(), command.email())).thenReturn(true);

        ServiceException exception = assertThrows(
                ServiceException.class,
                () -> authenticationService.register(command)
        );

        assertEquals("Người dùng đã tồn tại", exception.getMessage());
        verify(userRepository).existsByUsernameOrEmail(command.username(), command.email());
    }

    @Test
    void registerVenueOwnerCreatesAccountWithVenueOwnerRole() throws ServiceException {
        RegisterRequestCommand command = new RegisterRequestCommand(
                "goat.owner",
                "Nguyễn Minh",
                "owner@example.com",
                "encrypted-password",
                "encrypted-confirm-password"
        );
        Role venueOwnerRole = Role.builder().name(RoleEnum.VENUE_OWNER.name()).build();

        when(userRepository.existsByUsernameOrEmail(command.username(), command.email())).thenReturn(false);
        when(rsaPort.decrypt(command.password())).thenReturn("Strong@123");
        when(rsaPort.decrypt(command.confirmPassword())).thenReturn("Strong@123");
        when(jwtPort.hashPassword("Strong@123")).thenReturn("hashed-password");
        when(roleRepository.findByName(RoleEnum.VENUE_OWNER.name())).thenReturn(java.util.Optional.of(venueOwnerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(java.util.UUID.randomUUID());
            return user;
        });
        when(jwtPort.getAuthProperties()).thenReturn(new AuthProperties(300, 3600));

        authenticationService.registerVenueOwner(command);

        verify(roleRepository).findByName(RoleEnum.VENUE_OWNER.name());
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(
                user -> user.getRole() == venueOwnerRole && user.getStatus() != null
        ));
    }
}
