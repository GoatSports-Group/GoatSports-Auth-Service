package iuh.fit.auth.infrastructure.notification;

import iuh.fit.auth.application.port.notification.AsyncEmailPort;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
public class EmailNotificationAdapter implements EmailNotificationPort {
    private final SpringTemplateEngine templateEngine;
    private final AsyncEmailPort asyncEmailPort;

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
}
