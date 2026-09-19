package iuh.fit.auth.application.service;

import iuh.fit.auth.application.exception.RateLimitExceededException;
import iuh.fit.auth.application.port.cache.RedisPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthRateLimiterTest {
    @Mock RedisPort redisPort;

    private AuthRateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new AuthRateLimiter(redisPort);
        ReflectionTestUtils.setField(limiter, "loginMaxAttempts", 5);
        ReflectionTestUtils.setField(limiter, "loginWindowSeconds", 60L);
        ReflectionTestUtils.setField(limiter, "otpMaxAttempts", 3);
        ReflectionTestUtils.setField(limiter, "otpWindowSeconds", 3600L);
        ReflectionTestUtils.setField(limiter, "passwordResetMaxAttempts", 5);
        ReflectionTestUtils.setField(limiter, "passwordResetWindowSeconds", 86400L);
    }

    @Test
    void attemptAtTheLimitIsStillAllowed() {
        when(redisPort.incrementWithTtl(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(5L);

        assertThatCode(() -> limiter.checkLogin("player@goatsports.vn")).doesNotThrowAnyException();
    }

    @Test
    void attemptBeyondTheLimitIsRejected() {
        when(redisPort.incrementWithTtl(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(6L);

        assertThatThrownBy(() -> limiter.checkLogin("player@goatsports.vn"))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void counterKeyIsNormalisedSoCaseAndSpacingCannotBypassTheLimit() {
        when(redisPort.incrementWithTtl(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(1L);

        limiter.checkLogin("  Player@GoatSports.VN ");

        verify(redisPort).incrementWithTtl(
                eq("ratelimit:login:player@goatsports.vn"), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    void separateActionsUseSeparateCounters() {
        when(redisPort.incrementWithTtl(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(1L);

        limiter.checkOtpResend("player@goatsports.vn");
        limiter.checkPasswordReset("player@goatsports.vn");

        verify(redisPort).incrementWithTtl(
                eq("ratelimit:otp:player@goatsports.vn"), eq(3600L), eq(TimeUnit.SECONDS));
        verify(redisPort).incrementWithTtl(
                eq("ratelimit:password-reset:player@goatsports.vn"), eq(86400L), eq(TimeUnit.SECONDS));
    }

    @Test
    void blankIdentityDoesNotConsumeACounter() {
        limiter.checkLogin("   ");

        verifyNoInteractions(redisPort);
    }

    @Test
    void otpLimitIsStricterThanLoginLimit() {
        when(redisPort.incrementWithTtl(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(4L);

        assertThatThrownBy(() -> limiter.checkOtpResend("player@goatsports.vn"))
                .isInstanceOf(RateLimitExceededException.class);
        assertThatCode(() -> limiter.checkLogin("player@goatsports.vn")).doesNotThrowAnyException();
    }
}
