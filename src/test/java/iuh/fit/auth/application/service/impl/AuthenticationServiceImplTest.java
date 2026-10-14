package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.AuthProperties;
import iuh.fit.auth.application.dto.request.auth.RegisterRequestCommand;
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
import iuh.fit.auth.domain.enums.UserStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

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
                RoleEnum.PLAYER,
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
    void registerWithVenueOwnerAccountTypeCreatesVenueOwnerRole() throws ServiceException {
        RegisterRequestCommand command = new RegisterRequestCommand(
                RoleEnum.VENUE_OWNER,
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
        when(jwtPort.getAuthProperties()).thenReturn(new AuthProperties(300, 3600));
        when(roleRepository.findByName(RoleEnum.VENUE_OWNER.name())).thenReturn(java.util.Optional.of(venueOwnerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(java.util.UUID.randomUUID());
            return user;
        });
        authenticationService.register(command);

        verify(roleRepository).findByName(RoleEnum.VENUE_OWNER.name());
        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(
                user -> user.getRole() == venueOwnerRole && user.getStatus() != null
        ));
        verify(emailNotificationPort).sendVerificationEmail(eq(command.email()), anyString());
        verify(redisPort).saveWithTTL(
                anyString(),
                anyString(),
                eq(300L),
                eq(java.util.concurrent.TimeUnit.SECONDS)
        );
    }

    @Test
    void registerRejectsAccountTypesOutsidePublicRegistrationRoles() {
        RegisterRequestCommand command = new RegisterRequestCommand(
                RoleEnum.ADMIN,
                "goat.admin",
                "Goat Admin",
                "admin@example.com",
                "encrypted-password",
                "encrypted-confirm-password"
        );

        assertThrows(ServiceException.class, () -> authenticationService.register(command));
        verifyNoInteractions(userRepository, roleRepository);
    }

    private User user(RoleEnum role, UserStatusEnum status) {
        User user = new User();
        user.setUserId(java.util.UUID.randomUUID());
        user.setStatus(status);
        Role userRole = new Role();
        userRole.setName(role.name());
        user.setRole(userRole);
        return user;
    }

    @Test
    void blockingAPlayerRevokesTheirRefreshSessions() throws ServiceException {
        User player = user(RoleEnum.PLAYER, UserStatusEnum.ACTIVE);
        when(currentUserProvider.getCurrentUserId()).thenReturn(java.util.Optional.of("admin-id"));
        when(userRepository.findById(player.getUserId())).thenReturn(java.util.Optional.of(player));

        authenticationService.changeUserStatus(player.getUserId(), UserStatusEnum.BLOCKED);

        assertEquals(UserStatusEnum.BLOCKED, player.getStatus());
        verify(userRepository).save(player);
        verify(redisPort).deleteKey("user:" + player.getUserId() + ":refresh");
        verify(redisPort).deleteKey("user:" + player.getUserId() + ":keycloakRefresh");
    }

    @Test
    void adminsAndTheActorThemselvesCannotBeBlocked() throws ServiceException {
        User admin = user(RoleEnum.ADMIN, UserStatusEnum.ACTIVE);
        when(currentUserProvider.getCurrentUserId()).thenReturn(java.util.Optional.of(admin.getUserId().toString()));
        assertThrows(ServiceException.class,
                () -> authenticationService.changeUserStatus(admin.getUserId(), UserStatusEnum.BLOCKED));

        User otherAdmin = user(RoleEnum.ADMIN, UserStatusEnum.ACTIVE);
        when(userRepository.findById(otherAdmin.getUserId())).thenReturn(java.util.Optional.of(otherAdmin));
        assertThrows(ServiceException.class,
                () -> authenticationService.changeUserStatus(otherAdmin.getUserId(), UserStatusEnum.BLOCKED));
        verify(userRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void onlyBlockedAccountsCanBeUnblockedAndOnlyThoseTwoStatesAreAllowed() throws ServiceException {
        User pending = user(RoleEnum.PLAYER, UserStatusEnum.INACTIVE);
        when(currentUserProvider.getCurrentUserId()).thenReturn(java.util.Optional.of("admin-id"));
        when(userRepository.findById(pending.getUserId())).thenReturn(java.util.Optional.of(pending));

        assertThrows(ServiceException.class,
                () -> authenticationService.changeUserStatus(pending.getUserId(), UserStatusEnum.ACTIVE));
        assertThrows(ServiceException.class,
                () -> authenticationService.changeUserStatus(pending.getUserId(), UserStatusEnum.PENDING));
        assertEquals(UserStatusEnum.INACTIVE, pending.getStatus());
    }
}
