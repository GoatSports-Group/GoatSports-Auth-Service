package iuh.fit.auth.application.exception;

/**
 * Vượt ngưỡng số lần thử cho một hành động nhạy cảm (đăng nhập, gửi lại OTP, đặt lại mật khẩu).
 * Tách khỏi ServiceException để client phân biệt được "thử quá nhiều" với "sai thông tin".
 */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
