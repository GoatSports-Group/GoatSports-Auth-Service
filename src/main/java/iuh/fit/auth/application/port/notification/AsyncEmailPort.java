package iuh.fit.auth.application.port.notification;

public interface AsyncEmailPort {
    void sendEmailSync(String recipient, String subject, String content, boolean isMultipart, boolean isHtml);

    void sendHtmlWithReplyTo(String recipient, String replyTo, String subject, String html);
}
