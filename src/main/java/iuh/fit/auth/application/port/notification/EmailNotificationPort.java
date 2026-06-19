package iuh.fit.auth.application.port.notification;

public interface EmailNotificationPort {
    void sendVerificationEmail(String email, String verificationCode);
}
