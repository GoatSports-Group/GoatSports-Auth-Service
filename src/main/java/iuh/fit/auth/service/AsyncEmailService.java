package iuh.fit.auth.service;

public interface AsyncEmailService {
    void sendEmailSync(String recipient, String subject, String content, boolean isMultipart, boolean isHtml);
}
