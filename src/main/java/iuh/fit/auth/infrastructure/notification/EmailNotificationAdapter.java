package iuh.fit.auth.infrastructure.notification;

import iuh.fit.auth.application.port.notification.AsyncEmailPort;
import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailNotificationAdapter implements EmailNotificationPort {
    private final SpringTemplateEngine templateEngine;
    private final AsyncEmailPort asyncEmailPort;

    private static final String TICKET_QR_PATH = "/venue-service/api/v1/venues/ticket-qr?code=";
    private static final Locale VI = Locale.forLanguageTag("vi-VN");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Hộp thư nhận yêu cầu hỗ trợ và thư khiếu nại (có thể khác tài khoản Gmail dùng để gửi). */
    @Value("${spring.mail.username}")
    private String supportInbox;

    @Value("${client.frontend.url}")
    private String clientUrl;

    @Value("${api.public.url}")
    private String apiPublicUrl;

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

    @Override
    public void sendBookingTicket(String email, String fullName, BookingTicketMail ticket) {
        String date = ticket.playDate().format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", VI));
        String slot = ticket.startTime().format(HOUR) + " – " + ticket.endTime().format(HOUR);
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("bookingCode", ticket.bookingCode());
        context.setVariable("date", date.substring(0, 1).toUpperCase(VI) + date.substring(1));
        context.setVariable("slot", slot);
        context.setVariable("venueName", ticket.venueName());
        context.setVariable("courtName", ticket.courtName());
        context.setVariable("venueAddress", ticket.venueAddress());
        context.setVariable("depositAmount", money(ticket.depositAmount()));
        context.setVariable("remainingAmount", money(ticket.remainingAmount()));
        context.setVariable("hasRemaining", ticket.remainingAmount() != null && ticket.remainingAmount() > 0);
        context.setVariable("detailUrl", clientUrl + "/booking/detail/" + ticket.bookingId());
        context.setVariable("qrImageUrl", apiPublicUrl + TICKET_QR_PATH
                + URLEncoder.encode(ticket.qrCode(), StandardCharsets.UTF_8));
        String html = this.templateEngine.process("booking-ticket", context);
        this.asyncEmailPort.sendEmailSync(email,
                "Vé đặt sân " + ticket.bookingCode() + " · " + slot + " " + ticket.playDate().format(DAY),
                html, false, true);
    }

    private static String money(Double amount) {
        return NumberFormat.getIntegerInstance(VI).format(amount == null ? 0 : amount) + " đ";
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
