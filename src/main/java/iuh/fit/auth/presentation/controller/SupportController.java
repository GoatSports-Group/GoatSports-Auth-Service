package iuh.fit.auth.presentation.controller;

import iuh.fit.auth.application.port.notification.EmailNotificationPort;
import iuh.fit.auth.application.service.AuthRateLimiter;
import iuh.fit.auth.presentation.request.support.SupportRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Trang "Lien he ho tro": chuyen yeu cau toi hop thu ho tro (app.support.inbox) qua Gmail SMTP, tra loi thu
 * di thang toi email nguoi gui. Cong khai (khach cung gui duoc), gioi han 5 lan / gio / email.
 */
@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
public class SupportController {
    private static final Map<String, String> TOPICS = Map.of(
            "BOOKING", "Đặt sân & thanh toán",
            "REFUND", "Hủy sân & hoàn tiền",
            "CLUB_TOURNAMENT", "Câu lạc bộ & giải đấu",
            "ACCOUNT", "Tài khoản & đăng nhập",
            "FEEDBACK", "Góp ý khác");

    private final EmailNotificationPort emailNotificationPort;
    private final AuthRateLimiter authRateLimiter;

    @PostMapping("/requests")
    public ResponseEntity<Void> submit(@Valid @RequestBody SupportRequest request) {
        authRateLimiter.checkSupportRequest(request.getEmail());
        emailNotificationPort.sendSupportRequest(new EmailNotificationPort.SupportRequestMail(
                request.getFullName().trim(), request.getEmail().trim(), TOPICS.get(request.getTopic()),
                request.getSubject().trim(), request.getMessage().trim()));
        return ResponseEntity.accepted().build();
    }
}
