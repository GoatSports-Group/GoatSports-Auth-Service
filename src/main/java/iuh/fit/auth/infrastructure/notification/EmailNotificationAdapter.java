package iuh.fit.auth.infrastructure.notification;

import iuh.fit.auth.application.port.notification.AsyncEmailPort;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailNotificationAdapter implements EmailNotificationPort {
    private final SpringTemplateEngine templateEngine;
    private final AsyncEmailPort asyncEmailPort;

    /** Hộp thư nhận yêu cầu hỗ trợ và thư khiếu nại (có thể khác tài khoản Gmail dùng để gửi). */
    @Value("${spring.mail.username}")
    private String supportInbox;

    @Override
    public void sendVerificationEmail(String email, String verificationCode) {
        String subject = "Xác thực người dùng";
        Context context = new Context();
        context.setVariable("verificationCode", verificationCode);
        String content = this.templateEngine.process("otp", context);

        this.asyncEmailPort.sendEmailSync(email, subject, content, false, true);
    }

    @Override
    public void sendPasswordUpdateEmail(String email, String fullName, String newPassword) {
        String subject = "Thông báo cập nhật mật khẩu - Goat Sports";
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("newPassword", newPassword);
        String content = this.templateEngine.process("update-password", context);

        this.asyncEmailPort.sendEmailSync(email, subject, content, false, true);
    }

    @Override
    public void sendDeactivateEmail(String email, String fullName, boolean verified) {
        String subject = verified ? "Thông báo kích hoạt tài khoản - Goat Sports" : "Thông báo hủy xác thực tài khoản - Goat Sports";
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("verified", verified);
        String content = this.templateEngine.process("verification", context);

        this.asyncEmailPort.sendEmailSync(email, subject, content, false, true);
    }

    @Override
    public void sendSupportRequest(SupportRequestMail request) {
        Context context = new Context();
        context.setVariable("fullName", request.fullName());
        context.setVariable("email", request.email());
        context.setVariable("topic", request.topic());
        context.setVariable("subject", request.subject());
        context.setVariable("message", request.message());
        String html = this.templateEngine.process("support-request", context);
        this.asyncEmailPort.sendHtmlWithReplyTo(supportInbox, request.email(),
                "[Hỗ trợ GOAT Sports] " + request.topic() + " · " + request.subject(), html);
    }

    @Override
    public void sendAccountAppeal(AccountAppealMail mail) {
        Context context = new Context();
        context.setVariable("fullName", mail.fullName());
        context.setVariable("email", mail.email());
        context.setVariable("subject", mail.subject());
        context.setVariable("message", mail.message());
        String html = this.templateEngine.process("account-appeal", context);
        this.asyncEmailPort.sendHtmlWithReplyTo(supportInbox, mail.email(),
                "[Khiếu nại tài khoản bị khóa] " + mail.subject(), html);
    }

    @Override
    public void sendAccountAppealReply(String email, String fullName, String subject, String reply) {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("subject", subject);
        context.setVariable("reply", reply);
        String html = this.templateEngine.process("account-appeal-reply", context);
        this.asyncEmailPort.sendHtmlWithReplyTo(email, supportInbox, "Re: " + subject, html);
    }

    @Override
    public void sendAccountDeletionCode(String email, String fullName, String code) {
        sendAccountDeletionMail(email, fullName, "CODE", "Mã xác nhận xóa tài khoản - Goat Sports", code, null);
    }

    @Override
    public void sendAccountDeletionScheduled(String email, String fullName, LocalDateTime purgeAt) {
        sendAccountDeletionMail(email, fullName, "SCHEDULED", "Tài khoản Goat Sports sẽ bị xóa", null,
                purgeAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    @Override
    public void sendAccountDeletionCancelled(String email, String fullName) {
        sendAccountDeletionMail(email, fullName, "CANCELLED", "Đã hủy yêu cầu xóa tài khoản - Goat Sports", null, null);
    }

    private void sendAccountDeletionMail(String email, String fullName, String kind, String subject, String code,
                                         String purgeAt) {
        Context context = new Context();
        context.setVariable("kind", kind);
        context.setVariable("fullName", fullName);
        context.setVariable("code", code);
        context.setVariable("purgeAt", purgeAt);
        String html = this.templateEngine.process("account-deletion", context);
        this.asyncEmailPort.sendEmailSync(email, subject, html, false, true);
    }
}
