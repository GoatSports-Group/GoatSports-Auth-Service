package iuh.fit.auth.application.service.impl;

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
import iuh.fit.auth.application.service.AccountDeletionService;
import iuh.fit.auth.application.util.BaseUtil;
import iuh.fit.auth.domain.entity.PlayerSportProfile;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AccountDeletionServiceImpl implements AccountDeletionService {
    /** Hợp đồng với api-gateway: giá trị này ở khóa blocked_user: nghĩa là chờ xóa, không phải bị khóa. */
    public static final String BLOCKED_REASON_DELETION = "deletion";
    static final String ADMIN_ACCOUNT = "ADMIN_ACCOUNT";

    private static final String CODE_PREFIX = "account-deletion:code:";
    private static final String ATTEMPT_PREFIX = "account-deletion:attempts:";
    private static final String SEND_RATE_PREFIX = "rate:account-deletion-code:";
    private static final long CODE_TTL_MINUTES = 10;
    private static final int MAX_SENDS_PER_HOUR = 3;
    private static final int MAX_ATTEMPTS = 5;

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionServiceImpl.class);

    private final UserRepository userRepository;
    private final PlayerSportProfileRepository profileRepository;
    private final AccountAppealRepository appealRepository;
    private final RedisPort redisPort;
    private final KeycloakPort keycloakPort;
    private final EmailNotificationPort emailNotificationPort;
    private final UserRegistrationEventPublisher eventPublisher;
    private final AccountDeletionBlockerPort blockerPort;
    private final CurrentUserProvider currentUserProvider;
    private final JwtPort jwtPort;
    private final Clock clock;

    public AccountDeletionServiceImpl(
            UserRepository userRepository, PlayerSportProfileRepository profileRepository,
            AccountAppealRepository appealRepository, RedisPort redisPort, KeycloakPort keycloakPort,
            EmailNotificationPort emailNotificationPort,
            UserRegistrationEventPublisher eventPublisher, AccountDeletionBlockerPort blockerPort,
            CurrentUserProvider currentUserProvider, JwtPort jwtPort, Clock clock) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.appealRepository = appealRepository;
        this.redisPort = redisPort;
        this.keycloakPort = keycloakPort;
        this.emailNotificationPort = emailNotificationPort;
        this.eventPublisher = eventPublisher;
        this.blockerPort = blockerPort;
        this.currentUserProvider = currentUserProvider;
        this.jwtPort = jwtPort;
        this.clock = clock;
    }

    @Override
    public List<String> currentBlockers() throws ServiceException {
        return blockers(currentUserProvider.getCurrentUser());
    }

    @Override
    public void sendCode() throws ServiceException {
        User user = activeCurrentUser();
        requireNoBlockers(user);
        if (redisPort.incrementWithTtl(SEND_RATE_PREFIX + user.getUserId(), 1, TimeUnit.HOURS) > MAX_SENDS_PER_HOUR) {
            throw new ServiceException("Bạn đã yêu cầu mã quá nhiều lần, vui lòng thử lại sau 1 giờ.");
        }
        String code = BaseUtil.generateVerificationCode();
        redisPort.saveWithTTL(CODE_PREFIX + user.getUserId(), code, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        redisPort.deleteKey(ATTEMPT_PREFIX + user.getUserId());
        emailNotificationPort.sendAccountDeletionCode(user.getEmail(), user.getFullName(), code);
    }

    @Override
    public LocalDateTime requestDeletion(String code) throws ServiceException {
        User user = activeCurrentUser();
        UUID userId = user.getUserId();
        String expected = redisPort.getValue(CODE_PREFIX + userId);
        if (expected == null) {
            throw new ServiceException("Mã xác nhận đã hết hạn, vui lòng gửi lại mã.");
        }
        if (code == null || !expected.equals(code.trim())) {
            // Mã 6 số: giới hạn số lần đoán, quá thì hủy mã để phải xin mã mới.
            if (redisPort.incrementWithTtl(ATTEMPT_PREFIX + userId, CODE_TTL_MINUTES, TimeUnit.MINUTES) >= MAX_ATTEMPTS) {
                redisPort.deleteKey(CODE_PREFIX + userId);
            }
            throw new ServiceException("Mã xác nhận không đúng.");
        }
        requireNoBlockers(user);
        redisPort.deleteKey(CODE_PREFIX + userId);
        redisPort.deleteKey(ATTEMPT_PREFIX + userId);

        LocalDateTime now = LocalDateTime.now(clock);
        user.setStatus(UserStatusEnum.PENDING_DELETION);
        user.setDeletionRequestedAt(now);
        userRepository.save(user);

        // Đăng xuất mọi thiết bị ngay: gateway chặn access token còn hạn, refresh token bị thu hồi.
        redisPort.saveWithTTL("blocked_user:" + userId, BLOCKED_REASON_DELETION,
                jwtPort.getAuthProperties().refreshTokenExpirationSecond(), TimeUnit.SECONDS);
        redisPort.deleteKey("user:" + userId + ":refresh");
        redisPort.deleteKey("user:" + userId + ":keycloakRefresh");

        LocalDateTime purgeAt = now.plusDays(GRACE_DAYS);
        try {
            emailNotificationPort.sendAccountDeletionScheduled(user.getEmail(), user.getFullName(), purgeAt);
        } catch (Exception e) {
            log.warn("Could not send deletion-scheduled email to user {}: {}", userId, e.getMessage());
        }
        return purgeAt;
    }

    @Override
    public List<UUID> findDueForPurge(int limit) {
        return userRepository.findIdsDueForDeletion(LocalDateTime.now(clock).minusDays(GRACE_DAYS), limit);
    }

    @Override
    public void purge(UUID userId) throws ServiceException {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getStatus() != UserStatusEnum.PENDING_DELETION || user.getDeletionRequestedAt() == null
                || user.getDeletionRequestedAt().isAfter(LocalDateTime.now(clock).minusDays(GRACE_DAYS))) {
            return;
        }
        // Trong 30 ngày người khác vẫn có thể kéo người này vào việc mới (vd chuyển quyền chủ CLB). Còn vướng thì để
        // nguyên trạng thái chờ xóa, lượt sau kiểm lại; admin thấy tài khoản "Chờ xóa" quá hạn trong danh sách.
        List<String> blockers = blockerPort.findBlockers(userId);
        if (!blockers.isEmpty()) {
            log.warn("Account {} is past its deletion date but still has blockers {}; will retry", userId, blockers);
            return;
        }

        if (user.getKeycloakId() != null) {
            keycloakPort.deleteUser(user.getKeycloakId());
        }
        for (PlayerSportProfile profile : profileRepository.findAllByUserId(userId)) {
            profileRepository.deleteByIdAndUserId(profile.getProfileId(), userId);
        }
        appealRepository.deleteByUserId(userId);

        // Soft delete: họ tên, ảnh, username, email, SĐT ở lại để đơn đặt sân, giao dịch, đánh giá, kết quả trận cũ vẫn
        // hiện đúng người (email/SĐT chỉ chính chủ và quản trị viên xem được). Không đăng nhập lại được nữa; email và
        // username được giải phóng vì đăng nhập / đăng ký bỏ qua tài khoản DELETED.
        user.setPassword(null);
        user.setDateOfBirth(null);
        user.setGender(null);
        user.setCountry(null);
        user.setKeycloakId(null);
        user.setStatus(UserStatusEnum.DELETED);
        userRepository.save(user);

        // Gửi đồng bộ trong giao dịch: Kafka lỗi thì rollback, lượt sau làm lại (mọi bước trên đều làm lại được).
        eventPublisher.publishUserDeleted(userId);
        log.info("Account {} soft-deleted", userId);
    }

    private User activeCurrentUser() throws ServiceException {
        User user = currentUserProvider.getCurrentUser();
        if (user.getStatus() != UserStatusEnum.ACTIVE) {
            throw new ServiceException("Tài khoản không ở trạng thái hoạt động.");
        }
        return user;
    }

    private void requireNoBlockers(User user) throws ServiceException {
        if (!blockers(user).isEmpty()) {
            throw new ServiceException("Bạn cần xử lý xong các mục còn dang dở trước khi xóa tài khoản.");
        }
    }

    private List<String> blockers(User user) throws ServiceException {
        List<String> blockers = new ArrayList<>();
        if (user.getRole() != null && RoleEnum.ADMIN.name().equals(user.getRole().getName())) {
            blockers.add(ADMIN_ACCOUNT);
        }
        blockers.addAll(blockerPort.findBlockers(user.getUserId()));
        return blockers;
    }
}
