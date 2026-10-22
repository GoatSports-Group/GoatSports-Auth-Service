package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.AuthProperties;
import iuh.fit.auth.application.dto.request.user.UpdateUserRequestCommand;
import iuh.fit.auth.application.dto.response.user.UserResponseCommand;
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
import iuh.fit.auth.application.port.security.RsaPort;
import iuh.fit.auth.application.port.client.StoragePort;
import iuh.fit.auth.domain.entity.Permission;
import iuh.fit.auth.domain.entity.Role;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.GenderEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Ai thấy gì trong hồ sơ người khác, và chỉ chính chủ sửa được hồ sơ của mình. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceImplTest {
    @Mock JwtPort jwtPort;
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserMapper userMapper;
    @Mock UserRegistrationEventPublisher eventPublisher;
    @Mock StoragePort storagePort;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock EmailNotificationPort emailNotificationPort;
    @Mock RsaPort rsaPort;
    @Mock RedisPort redisPort;
    @Mock SessionRevocationPort sessionRevocationPort;

    private UserServiceImpl service;
    private final UUID userId = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(jwtPort, userRepository, roleRepository, userMapper, eventPublisher, storagePort,
                currentUserProvider, emailNotificationPort, rsaPort, redisPort, sessionRevocationPort);
        Role role = Role.builder().roleId(UUID.randomUUID()).name("PLAYER")
                .permissions(Set.of(Permission.builder().name("CREATE_BOOKING").build())).build();
        user = User.builder().userId(userId).email("an@goat.test").username("an").fullName("Nguyễn An")
                .phone("0901234567").gender(GenderEnum.MALE).dateOfBirth(LocalDate.of(2000, 1, 1))
                .status(UserStatusEnum.ACTIVE).role(role).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponseCommand(user)).thenReturn(new UserResponseCommand(userId, UUID.randomUUID(),
                "an@goat.test", "an", "hash", "Nguyễn An", "https://cdn/avatar.png", LocalDate.of(2000, 1, 1),
                "0901234567", "VN", UserStatusEnum.ACTIVE, GenderEnum.MALE, Set.of(), role,
                LocalDateTime.now(), LocalDateTime.now()));
    }

    private void callerIs(UUID callerId, boolean canViewUsers) {
        when(currentUserProvider.getCurrentUserId()).thenReturn(Optional.of(callerId.toString()));
        when(currentUserProvider.getCurrentUserEmailOrUsername()).thenReturn(Optional.of("other@goat.test"));
        when(currentUserProvider.hasAuthority("GET_USERS")).thenReturn(canViewUsers);
    }

    @Test
    void otherUsersSeeOnlyThePublicProfile() throws ServiceException {
        callerIs(UUID.randomUUID(), false);

        UserResponseCommand seen = service.getVisibleUser(userId.toString());

        assertThat(seen.fullName()).isEqualTo("Nguyễn An");
        assertThat(seen.username()).isEqualTo("an");
        assertThat(seen.avatarUrl()).isEqualTo("https://cdn/avatar.png");
        assertThat(seen.email()).isNull();
        assertThat(seen.phone()).isNull();
        assertThat(seen.dateOfBirth()).isNull();
        assertThat(seen.gender()).isNull();
        assertThat(seen.country()).isNull();
        assertThat(seen.keycloakId()).isNull();
        assertThat(seen.password()).isNull();
        assertThat(seen.role().getName()).isEqualTo("PLAYER");
        assertThat(seen.role().getPermissions()).isNull();
    }

    @Test
    void theOwnerAndStaffWithGetUsersSeeTheFullProfile() throws ServiceException {
        callerIs(userId, false);
        assertThat(service.getVisibleUser(userId.toString()).phone()).isEqualTo("0901234567");

        callerIs(UUID.randomUUID(), true);
        assertThat(service.getVisibleUser(userId.toString()).email()).isEqualTo("an@goat.test");
    }

    @Test
    void nobodyEditsSomeoneElsesProfileOrAvatar() throws ServiceException {
        callerIs(UUID.randomUUID(), true); // kể cả có GET_USERS: admin sửa qua /admin/users

        assertThatThrownBy(() -> service.updateUser(userId.toString(),
                new UpdateUserRequestCommand("hacked", "Hacked", "000", "VN", "MALE")))
                .isInstanceOf(ServiceException.class).hasMessageContaining("chính mình");
        assertThatThrownBy(() -> service.updateAvatar(userId.toString(), "temp/avatars/x/y-z.png"))
                .isInstanceOf(ServiceException.class).hasMessageContaining("chính mình");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(storagePort);
        assertThat(user.getFullName()).isEqualTo("Nguyễn An");
    }

    @Test
    void theOwnerStillEditsTheirOwnProfile() throws ServiceException {
        callerIs(userId, false);
        when(userRepository.save(user)).thenReturn(user);

        service.updateUser(userId.toString(), new UpdateUserRequestCommand("an", "Nguyễn Văn An", "0909", "VN", "MALE"));

        assertThat(user.getFullName()).isEqualTo("Nguyễn Văn An");
    }

    /** "Xác thực lại" một tài khoản chờ xóa sẽ âm thầm hủy việc xóa: admin không được làm vậy. */
    @Test
    void adminCannotTouchAnAccountThatIsBeingDeleted() throws ServiceException {
        user.setStatus(UserStatusEnum.PENDING_DELETION);

        assertThatThrownBy(() -> service.verifyUser(userId.toString(), true))
                .isInstanceOf(ServiceException.class).hasMessageContaining("chờ xóa");
        assertThat(user.getStatus()).isEqualTo(UserStatusEnum.PENDING_DELETION);
        verify(userRepository, never()).save(any());
    }

    /** Admin bỏ xác thực: tab đang mở đăng xuất ngay, gateway chặn token còn hạn bằng mã riêng; xác thực lại thì gỡ chặn. */
    @Test
    void unverifyingAnAccountEndsItsSessionsAndReverifyingLiftsTheGatewayFlag() throws ServiceException {
        when(userRepository.save(user)).thenReturn(user);
        when(jwtPort.getAuthProperties()).thenReturn(new AuthProperties(300, 3600));

        service.verifyUser(userId.toString(), false);

        assertThat(user.getStatus()).isEqualTo(UserStatusEnum.INACTIVE);
        verify(redisPort).saveWithTTL("blocked_user:" + userId, "inactive", 3600L, TimeUnit.SECONDS);
        verify(redisPort).deleteKey("user:" + userId + ":refresh");
        verify(sessionRevocationPort).notifySessionRevoked(userId, "ACCOUNT_INACTIVE");

        service.verifyUser(userId.toString(), true);

        assertThat(user.getStatus()).isEqualTo(UserStatusEnum.ACTIVE);
        verify(redisPort).deleteKey("blocked_user:" + userId);
    }
}
