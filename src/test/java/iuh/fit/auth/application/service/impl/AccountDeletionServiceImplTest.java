package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.AuthProperties;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.cache.RedisPort;
import iuh.fit.auth.application.port.client.AccountDeletionBlockerPort;
import iuh.fit.auth.application.port.event.UserRegistrationEventPublisher;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.port.persistence.AccountAppealRepository;
import iuh.fit.auth.application.port.persistence.PlayerSportProfileRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.application.port.security.JwtPort;
import iuh.fit.auth.application.port.security.KeycloakPort;
import iuh.fit.auth.domain.entity.PlayerSportProfile;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceImplTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T03:00:00Z"), ZONE);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    @Mock private UserRepository userRepository;
    @Mock private PlayerSportProfileRepository profileRepository;
    @Mock private AccountAppealRepository appealRepository;
    @Mock private RedisPort redisPort;
    @Mock private KeycloakPort keycloakPort;
    @Mock private EmailNotificationPort emailNotificationPort;
    @Mock private UserRegistrationEventPublisher eventPublisher;
    @Mock private AccountDeletionBlockerPort blockerPort;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private JwtPort jwtPort;
    @Mock private iuh.fit.auth.application.port.client.SessionRevocationPort sessionRevocationPort;

    private AccountDeletionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountDeletionServiceImpl(userRepository, profileRepository, appealRepository, redisPort,
                keycloakPort, emailNotificationPort, eventPublisher, blockerPort, currentUserProvider,
                jwtPort, sessionRevocationPort, CLOCK);
    }

    private static User user(RoleEnum roleName, UserStatusEnum status) {
        Role role = new Role();
        role.setName(roleName.name());
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setKeycloakId(UUID.randomUUID());
        user.setEmail("player@example.com");
        user.setUsername("player");
        user.setFullName("Nguyễn Minh");
        user.setPassword("bcrypt-hash");
        user.setPhone("0900000000");
        user.setAvatarUrl("https://cdn.example/avatars/a.png");
        user.setStatus(status);
        user.setRole(role);
        return user;
    }

    @Test
    void theRightCodePutsTheAccountIntoTheThirtyDayWaitAndLogsItOutEverywhere() throws ServiceException {
        User player = user(RoleEnum.PLAYER, UserStatusEnum.ACTIVE);
        when(currentUserProvider.getCurrentUser()).thenReturn(player);
        when(redisPort.getValue("account-deletion:code:" + player.getUserId())).thenReturn("123456");
        when(blockerPort.findBlockers(player.getUserId())).thenReturn(List.of());
        when(jwtPort.getAuthProperties()).thenReturn(new AuthProperties(300, 3600));

        LocalDateTime purgeAt = service.requestDeletion(" 123456 ");

        assertEquals(NOW.plusDays(30), purgeAt);
        assertEquals(UserStatusEnum.PENDING_DELETION, player.getStatus());
        assertEquals(NOW, player.getDeletionRequestedAt());
        verify(userRepository).save(player);
        verify(redisPort).saveWithTTL("blocked_user:" + player.getUserId(), "deletion", 3600L, TimeUnit.SECONDS);
        verify(redisPort).deleteKey("user:" + player.getUserId() + ":refresh");
        verify(sessionRevocationPort).notifySessionRevoked(player.getUserId(), "ACCOUNT_DELETION_PENDING");
        verify(emailNotificationPort).sendAccountDeletionScheduled(player.getEmail(), player.getFullName(), purgeAt);
    }

    @Test
    void aWrongCodeIsRejectedAndTooManyGuessesBurnTheCode() throws ServiceException {
        User player = user(RoleEnum.PLAYER, UserStatusEnum.ACTIVE);
        when(currentUserProvider.getCurrentUser()).thenReturn(player);
        when(redisPort.getValue("account-deletion:code:" + player.getUserId())).thenReturn("123456");
        when(redisPort.incrementWithTtl(eq("account-deletion:attempts:" + player.getUserId()), eq(10L), any()))
                .thenReturn(5L);

        assertThrows(ServiceException.class, () -> service.requestDeletion("000000"));

        assertEquals(UserStatusEnum.ACTIVE, player.getStatus());
        verify(redisPort).deleteKey("account-deletion:code:" + player.getUserId());
        verify(userRepository, never()).save(any());
    }

    @Test
    void noCodeIsSentWhileSomethingIsStillOpenOrForAdmins() throws ServiceException {
        User player = user(RoleEnum.PLAYER, UserStatusEnum.ACTIVE);
        when(currentUserProvider.getCurrentUser()).thenReturn(player);
        when(blockerPort.findBlockers(player.getUserId())).thenReturn(List.of("UPCOMING_BOOKINGS"));
        assertThrows(ServiceException.class, () -> service.sendCode());

        User admin = user(RoleEnum.ADMIN, UserStatusEnum.ACTIVE);
        when(currentUserProvider.getCurrentUser()).thenReturn(admin);
        when(blockerPort.findBlockers(admin.getUserId())).thenReturn(List.of());
        assertEquals(List.of("ADMIN_ACCOUNT"), service.currentBlockers());

        verify(emailNotificationPort, never()).sendAccountDeletionCode(anyString(), anyString(), anyString());
    }

    @Test
    void purgeKeepsTheNameForOldRecordsButEndsTheLoginAndTellsOtherServices() throws ServiceException {
        User pending = user(RoleEnum.PLAYER, UserStatusEnum.PENDING_DELETION);
        pending.setDeletionRequestedAt(NOW.minusDays(31));
        UUID keycloakId = pending.getKeycloakId();
        PlayerSportProfile profile = new PlayerSportProfile();
        profile.setProfileId(UUID.randomUUID());
        when(userRepository.findById(pending.getUserId())).thenReturn(Optional.of(pending));
        when(blockerPort.findBlockers(pending.getUserId())).thenReturn(List.of());
        when(profileRepository.findAllByUserId(pending.getUserId())).thenReturn(List.of(profile));

        service.purge(pending.getUserId());

        verify(keycloakPort).deleteUser(keycloakId);
        verify(profileRepository).deleteByIdAndUserId(profile.getProfileId(), pending.getUserId());
        verify(appealRepository).deleteByUserId(pending.getUserId());
        assertEquals(UserStatusEnum.DELETED, pending.getStatus());
        // Soft delete: đơn đặt sân, giao dịch, đánh giá cũ vẫn hiện đúng tên và ảnh.
        assertEquals("Nguyễn Minh", pending.getFullName());
        assertEquals("https://cdn.example/avatars/a.png", pending.getAvatarUrl());
        assertEquals("player@example.com", pending.getEmail());
        assertNull(pending.getPassword());
        assertNull(pending.getKeycloakId());
        verify(eventPublisher).publishUserDeleted(pending.getUserId());
    }

    @Test
    void purgeWaitsForTheFullGracePeriodAndForNewBlockers() throws ServiceException {
        User recent = user(RoleEnum.PLAYER, UserStatusEnum.PENDING_DELETION);
        recent.setDeletionRequestedAt(NOW.minusDays(29));
        when(userRepository.findById(recent.getUserId())).thenReturn(Optional.of(recent));
        service.purge(recent.getUserId());

        User stuck = user(RoleEnum.PLAYER, UserStatusEnum.PENDING_DELETION);
        stuck.setDeletionRequestedAt(NOW.minusDays(40));
        when(userRepository.findById(stuck.getUserId())).thenReturn(Optional.of(stuck));
        when(blockerPort.findBlockers(stuck.getUserId())).thenReturn(List.of("CLUB_OWNER"));
        service.purge(stuck.getUserId());

        assertEquals(UserStatusEnum.PENDING_DELETION, recent.getStatus());
        assertEquals(UserStatusEnum.PENDING_DELETION, stuck.getStatus());
        verify(keycloakPort, never()).deleteUser(any());
        verify(eventPublisher, never()).publishUserDeleted(any());
    }
}
