package iuh.fit.auth.application.port.notification;

import java.time.LocalDateTime;

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

    /** Mã xác nhận yêu cầu xóa tài khoản. */
    void sendAccountDeletionCode(String email, String fullName, String code);

    /** Đã nhận yêu cầu xóa: ngày xóa thật và cách hủy (đăng nhập lại). */
    void sendAccountDeletionScheduled(String email, String fullName, LocalDateTime purgeAt);

    /** Đăng nhập lại trong thời gian chờ nên yêu cầu xóa đã hủy. */
    void sendAccountDeletionCancelled(String email, String fullName);
}
