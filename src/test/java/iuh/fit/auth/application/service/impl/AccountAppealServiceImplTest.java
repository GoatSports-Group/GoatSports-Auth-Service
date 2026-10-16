package iuh.fit.auth.application.service.impl;

import iuh.fit.auth.application.dto.response.base.PageResult;
import iuh.fit.auth.application.exception.RateLimitExceededException;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.cache.RedisPort;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.port.persistence.AccountAppealRepository;
import iuh.fit.auth.application.port.persistence.UserRepository;
import iuh.fit.auth.application.port.security.CurrentUserProvider;
import iuh.fit.auth.domain.entity.AccountAppeal;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.domain.enums.AccountAppealStatus;
import iuh.fit.auth.domain.enums.UserStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountAppealServiceImplTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-16T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final String MESSAGE = "Tôi không vi phạm quy định, xin xem xét mở khóa giúp tôi.";

    @Mock
    private AccountAppealRepository appealRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RedisPort redisPort;
    @Mock
    private EmailNotificationPort emailNotificationPort;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private AccountAppealServiceImpl service;
    private User blocked;

    @BeforeEach
    void setUp() {
        service = new AccountAppealServiceImpl(appealRepository, userRepository, redisPort, emailNotificationPort,
                currentUserProvider, CLOCK);
        blocked = new User();
        blocked.setUserId(UUID.randomUUID());
        blocked.setEmail("bi.khoa@goat.test");
        blocked.setFullName("Người Bị Khóa");
        blocked.setStatus(UserStatusEnum.BLOCKED);
    }

    private void tokenFor(User user) {
        when(redisPort.hasKey("account-appeal:token:tok")).thenReturn(true);
        when(redisPort.getValue("account-appeal:token:tok")).thenReturn(user.getUserId().toString());
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
    }

    @Test
    void aValidTokenFilesTheAppealOnceAndForwardsItToTheSupportInbox() throws ServiceException {
        tokenFor(blocked);
        when(redisPort.incrementWithTtl("rate:account-appeal:" + blocked.getUserId(), 1, TimeUnit.DAYS)).thenReturn(1L);
        when(appealRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AccountAppeal appeal = service.submit("tok", "  Xin mở khóa  ", MESSAGE);

        assertEquals("Xin mở khóa", appeal.getSubject());
        assertEquals(AccountAppealStatus.NEW, appeal.getStatus());
        assertEquals(blocked.getEmail(), appeal.getEmail());
        assertEquals(LocalDateTime.of(2026, 10, 16, 10, 0), appeal.getCreatedAt());
        verify(redisPort).deleteKey("account-appeal:token:tok");  // dùng một lần
        ArgumentCaptor<EmailNotificationPort.AccountAppealMail> mail =
                ArgumentCaptor.forClass(EmailNotificationPort.AccountAppealMail.class);
        verify(emailNotificationPort).sendAccountAppeal(mail.capture());
        assertEquals(blocked.getEmail(), mail.getValue().email());
    }

    @Test
    void anUnknownOrExpiredTokenIsRejected() {
        when(redisPort.hasKey("account-appeal:token:tok")).thenReturn(false);
        assertThrows(ServiceException.class, () -> service.submit("tok", "Xin mở khóa", MESSAGE));
        verify(appealRepository, never()).save(any());
    }

    @Test
    void anAccountThatIsNoLongerBlockedCannotFile() {
        blocked.setStatus(UserStatusEnum.ACTIVE);
        tokenFor(blocked);
        assertThrows(ServiceException.class, () -> service.submit("tok", "Xin mở khóa", MESSAGE));
        verify(appealRepository, never()).save(any());
    }

    @Test
    void appealsPerAccountArePerDayLimited() {
        tokenFor(blocked);
        when(redisPort.incrementWithTtl(anyString(), eq(1L), eq(TimeUnit.DAYS))).thenReturn(4L);
        assertThrows(RateLimitExceededException.class, () -> service.submit("tok", "Xin mở khóa", MESSAGE));
        verify(appealRepository, never()).save(any());
    }

    @Test
    void tooShortMessagesAreRejectedBeforeTouchingTheToken() {
        assertThrows(ServiceException.class, () -> service.submit("tok", "Xin mở khóa", "ngắn"));
        verify(redisPort, never()).hasKey(anyString());
    }

    @Test
    void openingANewAppealMarksItReadAndShowsTheCurrentAccountStatus() throws ServiceException {
        AccountAppeal appeal = AccountAppeal.builder().appealId(UUID.randomUUID()).userId(blocked.getUserId())
                .status(AccountAppealStatus.NEW).build();
        when(appealRepository.findById(appeal.getAppealId())).thenReturn(Optional.of(appeal));
        when(appealRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findById(blocked.getUserId())).thenReturn(Optional.of(blocked));

        var detail = service.open(appeal.getAppealId());

        assertEquals(AccountAppealStatus.READ, detail.appeal().getStatus());
        assertEquals(UserStatusEnum.BLOCKED, detail.accountStatus());
    }

    @Test
    void replyingEmailsTheUserAndCanResolve() throws ServiceException {
        AccountAppeal appeal = AccountAppeal.builder().appealId(UUID.randomUUID()).userId(blocked.getUserId())
                .email(blocked.getEmail()).fullName(blocked.getFullName()).subject("Xin mở khóa")
                .status(AccountAppealStatus.READ).build();
        when(appealRepository.findById(appeal.getAppealId())).thenReturn(Optional.of(appeal));
        when(appealRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserProvider.getCurrentUserId()).thenReturn(Optional.of("admin-1"));

        AccountAppeal replied = service.reply(appeal.getAppealId(), "Đã mở khóa cho bạn.", true);

        assertEquals(AccountAppealStatus.RESOLVED, replied.getStatus());
        assertEquals("admin-1", replied.getRepliedBy());
        verify(emailNotificationPort).sendAccountAppealReply(blocked.getEmail(), blocked.getFullName(),
                "Xin mở khóa", "Đã mở khóa cho bạn.");
    }

    @Test
    void listingClampsThePageSize() {
        when(appealRepository.find(null, 1, 50)).thenReturn(new PageResult<>(java.util.List.of(), 0, 1, 50, 0));
        service.list(null, 0, 500);
        verify(appealRepository).find(null, 1, 50);
    }
}
