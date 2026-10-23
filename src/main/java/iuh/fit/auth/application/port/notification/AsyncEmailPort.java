package iuh.fit.auth.application.port.notification;

public interface AsyncEmailPort {
    void sendEmailSync(String recipient, String subject, String content, boolean isMultipart, boolean isHtml);

    void sendHtmlWithReplyTo(String recipient, String replyTo, String subject, String html);

    /** HTML kèm một ảnh nhúng, gọi trong HTML bằng {@code cid:<contentId>} (Gmail chặn ảnh {@code data:}). */
    void sendHtmlWithInlineImage(String recipient, String subject, String html, String contentId, byte[] png);
}
