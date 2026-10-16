package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.exception.RateLimitExceededException;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.cache.RedisPort;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.port.persistence.AccountAppealRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.application.service.AccountAppealService;
import iuh.fit.auth.domain.entity.AccountAppeal;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.AccountAppealStatus;
import iuh.fit.auth.domain.enums.UserStatusEnum;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AccountAppealServiceImpl implements AccountAppealService {
    static final String TOKEN_PREFIX = "account-appeal:token:";
    static final String RATE_PREFIX = "rate:account-appeal:";
    private static final long TOKEN_TTL_MINUTES = 30;
    /** Mỗi tài khoản tối đa ngần này thư mỗi ngày, để hộp thư quản trị không bị spam. */
    private static final int MAX_APPEALS_PER_DAY = 3;

    private final AccountAppealRepository appealRepository;
    private final UserRepository userRepository;
    private final RedisPort redisPort;
    private final EmailNotificationPort emailNotificationPort;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public AccountAppealServiceImpl(AccountAppealRepository appealRepository, UserRepository userRepository,
                                    RedisPort redisPort, EmailNotificationPort emailNotificationPort,
                                    CurrentUserProvider currentUserProvider, Clock clock) {
        this.appealRepository = appealRepository;
        this.userRepository = userRepository;
        this.redisPort = redisPort;
        this.emailNotificationPort = emailNotificationPort;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Override
    public String issueAppealToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        redisPort.saveWithTTL(TOKEN_PREFIX + token, userId.toString(), TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        return token;
    }

    @Override
    public long appealTokenTtlMinutes() {
        return TOKEN_TTL_MINUTES;
    }

    @Override
    public AccountAppeal submit(String appealToken, String subject, String message) throws ServiceException {
        String cleanSubject = subject == null ? "" : subject.strip();
        String cleanMessage = message == null ? "" : message.strip();
        if (cleanSubject.isEmpty() || cleanSubject.length() > SUBJECT_MAX) {
            throw new ServiceException("Tiêu đề cần từ 1 đến " + SUBJECT_MAX + " ký tự.");
        }
        if (cleanMessage.length() < MESSAGE_MIN || cleanMessage.length() > MESSAGE_MAX) {
            throw new ServiceException("Nội dung cần từ " + MESSAGE_MIN + " đến " + MESSAGE_MAX + " ký tự.");
        }
        String key = TOKEN_PREFIX + (appealToken == null ? "" : appealToken.strip());
        String userId = appealToken == null || !redisPort.hasKey(key) ? null : redisPort.getValue(key);
        if (userId == null) {
            throw new ServiceException("Phiên gửi khiếu nại đã hết hạn. Hãy đăng nhập lại để gửi.");
        }
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ServiceException("Tài khoản không tồn tại."));
        if (user.getStatus() != UserStatusEnum.BLOCKED) {
            redisPort.deleteKey(key);
            throw new ServiceException("Tài khoản không còn bị khóa. Hãy đăng nhập lại.");
        }
        if (redisPort.incrementWithTtl(RATE_PREFIX + userId, 1, TimeUnit.DAYS) > MAX_APPEALS_PER_DAY) {
            throw new RateLimitExceededException("Bạn đã gửi " + MAX_APPEALS_PER_DAY
                    + " thư trong hôm nay. Quản trị viên sẽ phản hồi qua email, vui lòng chờ.");
        }
        AccountAppeal saved = appealRepository.save(AccountAppeal.builder()
                .userId(user.getUserId()).email(user.getEmail()).fullName(user.getFullName())
                .subject(cleanSubject).message(cleanMessage).status(AccountAppealStatus.NEW)
                .createdAt(now()).build());
        // Mã dùng một lần: muốn gửi thêm thì phải nhập lại mật khẩu.
        redisPort.deleteKey(key);
        emailNotificationPort.sendAccountAppeal(new EmailNotificationPort.AccountAppealMail(
                user.getFullName(), user.getEmail(), cleanSubject, cleanMessage));
        return saved;
    }

    @Override
    public PageResult<AccountAppeal> list(AccountAppealStatus status, int page, int pageSize) {
        return appealRepository.find(status, Math.max(1, page), Math.max(1, Math.min(50, pageSize)));
    }

    @Override
    public long unreadCount() {
        return appealRepository.countByStatus(AccountAppealStatus.NEW);
    }

    @Override
    public AppealDetail open(UUID appealId) throws ServiceException {
        AccountAppeal appeal = find(appealId);
        if (appeal.getStatus() == AccountAppealStatus.NEW) {
            appeal = appealRepository.save(appeal.toBuilder().status(AccountAppealStatus.READ).readAt(now()).build());
        }
        UserStatusEnum accountStatus = userRepository.findById(appeal.getUserId()).map(User::getStatus).orElse(null);
        return new AppealDetail(appeal, accountStatus);
    }

    @Override
    public AccountAppeal reply(UUID appealId, String reply, boolean resolve) throws ServiceException {
        String text = reply == null ? "" : reply.strip();
        if (text.isEmpty() || text.length() > MESSAGE_MAX) {
            throw new ServiceException("Nội dung phản hồi cần từ 1 đến " + MESSAGE_MAX + " ký tự.");
        }
        AccountAppeal appeal = find(appealId);
        LocalDateTime now = now();
        AccountAppeal updated = appeal.toBuilder()
                .reply(text).repliedAt(now).repliedBy(currentUserProvider.getCurrentUserId().orElse(null))
                .status(resolve ? AccountAppealStatus.RESOLVED : AccountAppealStatus.READ)
                .readAt(appeal.getReadAt() == null ? now : appeal.getReadAt())
                .resolvedAt(resolve ? now : appeal.getResolvedAt())
                .build();
        AccountAppeal saved = appealRepository.save(updated);
        emailNotificationPort.sendAccountAppealReply(appeal.getEmail(), appeal.getFullName(), appeal.getSubject(), text);
        return saved;
    }

    @Override
    public AccountAppeal changeStatus(UUID appealId, AccountAppealStatus status) throws ServiceException {
        if (status == null || status == AccountAppealStatus.NEW) {
            throw new ServiceException("Chỉ đánh dấu được đã đọc hoặc đã xử lý.");
        }
        AccountAppeal appeal = find(appealId);
        LocalDateTime now = now();
        return appealRepository.save(appeal.toBuilder()
                .status(status)
                .readAt(appeal.getReadAt() == null ? now : appeal.getReadAt())
                .resolvedAt(status == AccountAppealStatus.RESOLVED ? now : null)
                .build());
    }

    private AccountAppeal find(UUID appealId) throws ServiceException {
        return appealRepository.findById(appealId).orElseThrow(() -> new ServiceException("Không tìm thấy thư."));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
