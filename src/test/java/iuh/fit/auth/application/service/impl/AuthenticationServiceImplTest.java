package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.AuthProperties;
import iuh.fit.auth.application.dto.request.auth.LoginRequestCommand;
import iuh.fit.auth.application.dto.request.auth.RegisterRequestCommand;
import iuh.fit.auth.application.dto.response.auth.KeycloakParseCommand;
import iuh.fit.auth.application.dto.response.auth.KeycloakResponseCommand;
import iuh.fit.auth.application.dto.response.auth.LoginResponseCommand;
import iuh.fit.auth.application.exception.AccountStatusException;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.mapper.UserMapper;
import iuh.fit.auth.application.port.cache.RedisPort;
import iuh.fit.auth.application.port.client.SessionRevocationPort;
import iuh.fit.auth.application.port.event.UserRegistrationEventPublisher;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.port.persistence.RoleRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.application.port.security.JwtPort;
import iuh.fit.auth.application.port.security.KeycloakPort;
import iuh.fit.auth.application.port.security.RsaPort;
import iuh.fit.auth.application.service.AccountAppealService;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    @Mock
    private AccountAppealService accountAppealService;
    @Mock
    private SessionRevocationPort sessionRevocationPort;

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
                rsaPort,
                accountAppealService,
                sessionRevocationPort
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
        when(roleRepository.findByName(RoleEnum.VENUE_OWNER.name())).thenReturn(Optional.of(venueOwnerRole));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(UUID.randomUUID());
            return user;
        });
        authenticationService.register(command);

        verify(roleRepository).findByName(RoleEnum.VENUE_OWNER.name());
        verify(userRepository).save(ArgumentMatchers.argThat(
                user -> user.getRole() == venueOwnerRole && user.getStatus() != null
        ));
        verify(emailNotificationPort).sendVerificationEmail(eq(command.email()), anyString());
        verify(redisPort).saveWithTTL(
                anyString(),
                anyString(),
                eq(300L),
                eq(TimeUnit.SECONDS)
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
        user.setUserId(UUID.randomUUID());
        user.setEmail(user.getUserId() + "@goat.test");
        user.setStatus(status);
        Role userRole = new Role();
        userRole.setName(role.name());
        user.setRole(userRole);
        return user;
    }

    @Test
    void blockingAPlayerRevokesTheirSessionsAndFlagsThemForTheGateway() throws ServiceException {
        User player = user(RoleEnum.PLAYER, UserStatusEnum.ACTIVE);
        when(currentUserProvider.getCurrentUserId()).thenReturn(Optional.of("admin-id"));
        when(userRepository.findById(player.getUserId())).thenReturn(Optional.of(player));
        when(jwtPort.getAuthProperties()).thenReturn(new AuthProperties(300, 3600));

        authenticationService.changeUserStatus(player.getUserId(), UserStatusEnum.BLOCKED);

        assertEquals(UserStatusEnum.BLOCKED, player.getStatus());
        verify(userRepository).save(player);
        verify(redisPort).deleteKey("user:" + player.getUserId() + ":refresh");
        verify(redisPort).deleteKey("user:" + player.getUserId() + ":keycloakRefresh");
        // Gateway đọc khóa này để chặn access token còn hạn (đăng xuất người đang dùng ngay).
        verify(redisPort).saveWithTTL("blocked_user:" + player.getUserId(), "1", 3600L, TimeUnit.SECONDS);
        // Tab đang ngồi yên cũng đăng xuất ngay qua websocket, không đợi request kế tiếp.
        verify(sessionRevocationPort).notifySessionRevoked(player.getUserId(), "ACCOUNT_BLOCKED");
    }

    @Test
    void unblockingClearsTheGatewayFlag() throws ServiceException {
        User blocked = user(RoleEnum.PLAYER, UserStatusEnum.BLOCKED);
        when(currentUserProvider.getCurrentUserId()).thenReturn(Optional.of("admin-id"));
        when(userRepository.findById(blocked.getUserId())).thenReturn(Optional.of(blocked));

        authenticationService.changeUserStatus(blocked.getUserId(), UserStatusEnum.ACTIVE);

        assertEquals(UserStatusEnum.ACTIVE, blocked.getStatus());
        verify(redisPort).deleteKey("blocked_user:" + blocked.getUserId());
    }

    @Test
    void adminsAndTheActorThemselvesCannotBeBlocked() throws ServiceException {
        User admin = user(RoleEnum.ADMIN, UserStatusEnum.ACTIVE);
        when(currentUserProvider.getCurrentUserId()).thenReturn(Optional.of(admin.getUserId().toString()));
        assertThrows(ServiceException.class,
                () -> authenticationService.changeUserStatus(admin.getUserId(), UserStatusEnum.BLOCKED));

        User otherAdmin = user(RoleEnum.ADMIN, UserStatusEnum.ACTIVE);
        when(userRepository.findById(otherAdmin.getUserId())).thenReturn(Optional.of(otherAdmin));
        assertThrows(ServiceException.class,
                () -> authenticationService.changeUserStatus(otherAdmin.getUserId(), UserStatusEnum.BLOCKED));
        verify(userRepository, Mockito.never()).save(any());
    }

    @Test
    void onlyBlockedAccountsCanBeUnblockedAndOnlyThoseTwoStatesAreAllowed() throws ServiceException {
        User pending = user(RoleEnum.PLAYER, UserStatusEnum.INACTIVE);
        when(currentUserProvider.getCurrentUserId()).thenReturn(Optional.of("admin-id"));
        when(userRepository.findById(pending.getUserId())).thenReturn(Optional.of(pending));

        assertThrows(ServiceException.class,
                () -> authenticationService.changeUserStatus(pending.getUserId(), UserStatusEnum.ACTIVE));
        assertThrows(ServiceException.class,
                () -> authenticationService.changeUserStatus(pending.getUserId(), UserStatusEnum.PENDING));
        assertEquals(UserStatusEnum.INACTIVE, pending.getStatus());
    }

    @Test
    void inactiveAccountWithTheRightPasswordGetsAnActivationCodeInsteadOfASession() throws ServiceException {
        User inactive = user(RoleEnum.PLAYER, UserStatusEnum.INACTIVE);
        when(userRepository.findByUsernameOrEmailWithRole(inactive.getEmail(), inactive.getEmail()))
                .thenReturn(Optional.of(inactive));
        when(userRepository.findByEmail(inactive.getEmail())).thenReturn(Optional.of(inactive));
        when(redisPort.incrementWithTtl(eq("rate:login-activation-otp:" + inactive.getUserId()), eq(1L), any()))
                .thenReturn(1L);
        when(jwtPort.getAuthProperties()).thenReturn(new AuthProperties(300, 3600));

        AccountStatusException error = assertThrows(AccountStatusException.class,
                () -> authenticationService.login(new LoginRequestCommand(
                        inactive.getEmail(), "secret")));

        assertEquals("ACCOUNT_INACTIVE", error.getCode());
        assertEquals(inactive.getEmail(), error.getData().get("email"));
        assertEquals(true, error.getData().get("codeSent"));
        verify(emailNotificationPort).sendVerificationEmail(eq(inactive.getEmail()), anyString());
        verifyNoInteractions(accountAppealService);
    }

    @Test
    void activationCodesFromLoginAreThrottled() throws ServiceException {
        User inactive = user(RoleEnum.PLAYER, UserStatusEnum.INACTIVE);
        when(userRepository.findByUsernameOrEmailWithRole(inactive.getEmail(), inactive.getEmail()))
                .thenReturn(Optional.of(inactive));
        when(redisPort.incrementWithTtl(anyString(), eq(1L), any())).thenReturn(4L);

        AccountStatusException error = assertThrows(AccountStatusException.class,
                () -> authenticationService.login(new LoginRequestCommand(
                        inactive.getEmail(), "secret")));

        assertEquals(false, error.getData().get("codeSent"));
        verifyNoInteractions(emailNotificationPort);
    }

    @Test
    void blockedAccountWithTheRightPasswordGetsAnAppealTokenInsteadOfASession() throws ServiceException {
        User blocked = user(RoleEnum.PLAYER, UserStatusEnum.BLOCKED);
        when(userRepository.findByUsernameOrEmailWithRole(blocked.getEmail(), blocked.getEmail()))
                .thenReturn(Optional.of(blocked));
        when(accountAppealService.issueAppealToken(blocked.getUserId())).thenReturn("appeal-token");
        when(accountAppealService.appealTokenTtlMinutes()).thenReturn(30L);

        AccountStatusException error = assertThrows(AccountStatusException.class,
                () -> authenticationService.login(new LoginRequestCommand(
                        blocked.getEmail(), "secret")));

        assertEquals("ACCOUNT_BLOCKED", error.getCode());
        assertEquals("appeal-token", error.getData().get("appealToken"));
        verifyNoInteractions(jwtPort);
    }

    @Test
    void googleLoginDoesNotBypassABlock() throws ServiceException {
        User blocked = user(RoleEnum.PLAYER, UserStatusEnum.BLOCKED);
        UUID keycloakId = UUID.randomUUID();
        blocked.setKeycloakId(keycloakId);
        when(keycloakPort.exchangeCode("code", "cb")).thenReturn(
                new KeycloakResponseCommand("kc-access", "kc-refresh", 300L, 1800L));
        when(jwtPort.decodeKeycloakToken("kc-access")).thenReturn(
                new KeycloakParseCommand(blocked.getEmail(), "blocked", "B", keycloakId));
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(blocked));
        when(accountAppealService.issueAppealToken(blocked.getUserId())).thenReturn("appeal-token");

        AccountStatusException error = assertThrows(AccountStatusException.class,
                () -> authenticationService.callback("code", "cb"));

        assertEquals("ACCOUNT_BLOCKED", error.getCode());
        verify(jwtPort, Mockito.never()).generateAccessToken(any());
    }

    /** Đăng nhập lại trong 30 ngày chờ xóa là hủy yêu cầu xóa (như Facebook) và mở lại cho gateway. */
    @Test
    void loggingInWhilePendingDeletionRestoresTheAccount() throws ServiceException {
        User pending = user(RoleEnum.PLAYER, UserStatusEnum.PENDING_DELETION);
        pending.setDeletionRequestedAt(LocalDateTime.now().minusDays(3));
        when(userRepository.findByUsernameOrEmailWithRole(pending.getEmail(), pending.getEmail()))
                .thenReturn(Optional.of(pending));
        when(jwtPort.getAuthProperties()).thenReturn(new AuthProperties(300, 3600));

        LoginResponseCommand session = authenticationService.login(new LoginRequestCommand(
                pending.getEmail(), "secret"));

        assertEquals(true, session.accountRestored());
        assertEquals(UserStatusEnum.ACTIVE, pending.getStatus());
        assertEquals(null, pending.getDeletionRequestedAt());
        verify(userRepository).save(pending);
        verify(redisPort).deleteKey("blocked_user:" + pending.getUserId());
        verify(emailNotificationPort).sendAccountDeletionCancelled(pending.getEmail(), pending.getFullName());
    }

    @Test
    void adminCannotBlockOrUnblockAnAccountPendingDeletion() throws ServiceException {
        User pending = user(RoleEnum.PLAYER, UserStatusEnum.PENDING_DELETION);
        when(currentUserProvider.getCurrentUserId()).thenReturn(Optional.of("admin-id"));
        when(userRepository.findById(pending.getUserId())).thenReturn(Optional.of(pending));

        assertThrows(ServiceException.class,
                () -> authenticationService.changeUserStatus(pending.getUserId(), UserStatusEnum.BLOCKED));
        assertEquals(UserStatusEnum.PENDING_DELETION, pending.getStatus());
    }

    /** Tài khoản chưa xác thực đăng nhập Google: nhận OTP và popup kích hoạt như đăng nhập mật khẩu, không cấp phiên. */
    @Test
    void googleLoginOfAnUnverifiedAccountGetsAnActivationCode() throws ServiceException {
        User inactive = user(RoleEnum.PLAYER, UserStatusEnum.INACTIVE);
        UUID keycloakId = UUID.randomUUID();
        inactive.setKeycloakId(keycloakId);
        when(keycloakPort.exchangeCode("code", "cb")).thenReturn(
                new KeycloakResponseCommand("kc-access", "kc-refresh", 300L, 1800L));
        when(jwtPort.decodeKeycloakToken("kc-access")).thenReturn(
                new KeycloakParseCommand(inactive.getEmail(), "inactive", "I", keycloakId));
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(inactive));
        when(userRepository.findByEmail(inactive.getEmail())).thenReturn(Optional.of(inactive));
        when(redisPort.incrementWithTtl(eq("rate:login-activation-otp:" + inactive.getUserId()), eq(1L), any())).thenReturn(1L);
        when(jwtPort.getAuthProperties()).thenReturn(new AuthProperties(300, 3600));

        AccountStatusException error = assertThrows(AccountStatusException.class,
                () -> authenticationService.callback("code", "cb"));

        assertEquals("ACCOUNT_INACTIVE", error.getCode());
        assertEquals(true, error.getData().get("codeSent"));
        verify(emailNotificationPort).sendVerificationEmail(eq(inactive.getEmail()), anyString());
        verify(jwtPort, Mockito.never()).generateAccessToken(any());
    }
}
