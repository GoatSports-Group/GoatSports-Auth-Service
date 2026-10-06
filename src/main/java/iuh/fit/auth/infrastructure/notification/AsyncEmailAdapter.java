package iuh.fit.auth.infrastructure.notification;

import iuh.fit.auth.application.port.notification.AsyncEmailPort;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncEmailAdapter implements AsyncEmailPort {
    private final JavaMailSender javaMailSender;

    @Override
    @Async("emailExecutor")
    public void sendEmailSync(String recipient, String subject, String content, boolean isMultipart, boolean isHtml) {
        MimeMessage mimeMessage = this.javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(content, isHtml);

            this.javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Lỗi gửi mail tới " + recipient + ": " + e.getMessage());
        }
    }

    @Override
    @Async("emailExecutor")
    public void sendHtmlWithReplyTo(String recipient, String replyTo, String subject, String html) {
        MimeMessage mimeMessage = this.javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name());
            helper.setTo(recipient);
            helper.setReplyTo(replyTo);
            helper.setSubject(subject);
            helper.setText(html, true);
            this.javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            log.error("Lỗi gửi mail tới " + recipient + ": " + e.getMessage());
        }
    }
}
