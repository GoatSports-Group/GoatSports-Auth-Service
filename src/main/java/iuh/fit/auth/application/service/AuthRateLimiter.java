package iuh.fit.auth.application.service;

import iuh.fit.auth.application.exception.RateLimitExceededException;
import iuh.fit.auth.application.port.cache.RedisPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Chặn brute-force trên các endpoint nhạy cảm bằng bộ đếm Redis có TTL.
 *
 * ponytail: giới hạn theo danh tính (email) — đủ chặn brute-force vào một tài khoản.
 * Nếu sau này gặp credential stuffing phân tán (nhiều email, một nguồn) thì thêm
 * bộ đếm theo IP lấy từ X-Forwarded-For của Gateway.
 */
@Service
@RequiredArgsConstructor
public class AuthRateLimiter {
    private static final String KEY_PREFIX = "ratelimit:";

    private final RedisPort redisPort;

    @Value("${app.rate-limit.login.max-attempts:5}")
    private int loginMaxAttempts;
    @Value("${app.rate-limit.login.window-seconds:60}")
    private long loginWindowSeconds;

    @Value("${app.rate-limit.otp.max-attempts:3}")
    private int otpMaxAttempts;
    @Value("${app.rate-limit.otp.window-seconds:3600}")
    private long otpWindowSeconds;

    @Value("${app.rate-limit.password-reset.max-attempts:5}")
    private int passwordResetMaxAttempts;
    @Value("${app.rate-limit.password-reset.window-seconds:86400}")
    private long passwordResetWindowSeconds;

    @Value("${app.rate-limit.support.max-attempts:5}")
    private int supportMaxAttempts;
    @Value("${app.rate-limit.support.window-seconds:3600}")
    private long supportWindowSeconds;

    public void checkSupportRequest(String email) {
        check("support", email, supportMaxAttempts, supportWindowSeconds,
                "Bạn đã gửi nhiều yêu cầu hỗ trợ trong một giờ. Vui lòng thử lại sau.");
    }

    public void checkLogin(String email) {
        check("login", email, loginMaxAttempts, loginWindowSeconds,
                "Bạn đã đăng nhập sai quá nhiều lần. Vui lòng thử lại sau ít phút.");
    }

    public void checkOtpResend(String email) {
        check("otp", email, otpMaxAttempts, otpWindowSeconds,
                "Bạn đã yêu cầu mã xác thực quá nhiều lần. Vui lòng thử lại sau.");
    }

    public void checkPasswordReset(String email) {
        check("password-reset", email, passwordResetMaxAttempts, passwordResetWindowSeconds,
                "Bạn đã yêu cầu đặt lại mật khẩu quá nhiều lần. Vui lòng thử lại sau.");
    }

    private void check(String action, String identity, int maxAttempts, long windowSeconds, String message) {
        if (!StringUtils.hasText(identity)) return;
        String key = KEY_PREFIX + action + ":" + identity.trim().toLowerCase(Locale.ROOT);
        if (redisPort.incrementWithTtl(key, windowSeconds, TimeUnit.SECONDS) > maxAttempts) {
            throw new RateLimitExceededException(message);
        }
    }
}
