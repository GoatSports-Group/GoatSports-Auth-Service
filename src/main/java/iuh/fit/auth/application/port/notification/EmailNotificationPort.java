package iuh.fit.auth.application.port.notification;

public interface EmailNotificationPort {
    void sendVerificationEmail(String email, String verificationCode);

    void sendPasswordUpdateEmail(String email, String fullName, String newPassword);

    void sendDeactivateEmail(String email, String fullName, boolean verified);

    void sendSupportRequest(SupportRequestMail request);

    record SupportRequestMail(String fullName, String email, String topic, String subject, String message) {
    }

    /** Thư khiếu nại của tài khoản bị khóa, gửi tới hộp thư hỗ trợ; "Trả lời" trong hộp thư đi thẳng tới người gửi. */
    void sendAccountAppeal(AccountAppealMail mail);

    /** Phản hồi của quản trị viên cho thư khiếu nại, gửi tới email tài khoản. */
    void sendAccountAppealReply(String email, String fullName, String subject, String reply);

    record AccountAppealMail(String fullName, String email, String subject, String message) {
    }
}
