package iuh.fit.auth.service;

public interface EmailNotificationService {
    void sendVerificationEmail(String email, String verificationCode);
}
