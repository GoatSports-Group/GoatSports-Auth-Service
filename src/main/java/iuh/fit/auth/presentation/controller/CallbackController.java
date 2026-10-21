package iuh.fit.auth.presentation.controller;

import iuh.fit.auth.application.exception.AccountStatusException;
import iuh.fit.auth.application.service.AuthenticationService;
import iuh.fit.auth.application.dto.response.auth.LoginResponseCommand;
import iuh.fit.auth.domain.enums.RoleEnum;
import iuh.fit.shared.dto.ApiLogEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CallbackController {

    private final AuthenticationService authenticationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${keycloak.callback-url}")
    private String callbackUrl;
    @Value("${client.frontend.url}")
    private String clientUrl;
    @Value("${admin.frontend.url}")
    private String adminUrl;
    @Value("${auth.frontend.url}")
    private String authUrl;
    @Value("${app.jwt.refresh-token.expiration-second}")
    private long refreshTokenExpirationSecond;
    @Value("${app.jwt.access-token.expiration-second}")
    private long accessTokenExpirationSecond;
    @Value("${app.cookie.secure}")
    private boolean cookieSecure;
    @Value("${app.cookie.domain}")
    private String cookieDomain;
    @Value("${spring.kafka.topic.user-log}")
    private String userLogTopic;

    @GetMapping("/callback")
    public void callback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception
    {
        LoginResponseCommand result;
        try {
            result = authenticationService.callback(code, callbackUrl);
        } catch (AccountStatusException blocked) {
            response.sendRedirect(authUrl.replaceAll("/+$", "") + "/login?" + blockedQuery(blocked));
            return;
        }

        // Ghi audit log thủ công sau khi đã có thông tin user
        sendAuditLog(result, request);

        ResponseCookie accessCookie = createCookie("accessToken", result.accessToken(), accessTokenExpirationSecond);
        ResponseCookie refreshCookie = createCookie("refreshToken", result.refreshToken(), refreshTokenExpirationSecond);

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        String redirectUrl = state == null || state.isBlank() ? clientUrl : state;
        if (result.user() != null && result.user().role() != null) {
            String roleName = result.user().role().getName();
            if (!RoleEnum.PLAYER.name().equals(roleName)) {
                redirectUrl = adminUrl.replaceAll("/+$", "") + "/admin/dashboard";
            }
        }

        response.sendRedirect(redirectUrl);
    }

    private void sendAuditLog(LoginResponseCommand result, HttpServletRequest request) {
        try {
            String userId = "anonymous";
            String username = "Khách ẩn danh";

            if (result.user() != null) {
                if (result.user().email() != null) {
                    userId = result.user().email();
                    username = result.user().email();
                } else if (result.user().username() != null) {
                    userId = result.user().username();
                    username = result.user().username();
                } else if (result.user().userId() != null) {
                    userId = result.user().userId().toString();
                    username = userId;
                }
            }

            String ipAddress = getIpAddress(request);
            String description = "Người dùng [" + username + "] đã đăng nhập thành công";

            ApiLogEvent event = ApiLogEvent.builder()
                    .userId(userId)
                    .action("LOGIN")
                    .description(description)
                    .ipAddress(ipAddress)
                    .statusCode(302)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(userLogTopic, event)
                    .whenComplete((sendResult, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send login audit log to Kafka: {}", ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            log.error("Error sending login audit log", ex);
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private ResponseCookie createCookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .domain(cookieDomain)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private static String blockedQuery(AccountStatusException blocked) {
        java.util.Map<String, Object> data = blocked.getData();
        StringBuilder query = new StringBuilder(
                AccountStatusException.INACTIVE.equals(blocked.getCode()) ? "account=inactive" : "account=blocked");
        for (String key : new String[]{"appealToken", "email", "fullName", "codeSent"}) {
            Object value = data.get(key);
            if (value != null) {
                query.append('&').append(key).append('=')
                        .append(java.net.URLEncoder.encode(value.toString(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return query.toString();
    }
}
