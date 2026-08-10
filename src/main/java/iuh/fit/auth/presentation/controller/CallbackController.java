package iuh.fit.auth.presentation.controller;

import iuh.fit.auth.application.service.AuthenticationService;
import iuh.fit.auth.application.dto.response.auth.LoginResponseCommand;
import iuh.fit.shared.annotation.AuditLog;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class CallbackController {

    private final AuthenticationService authenticationService;

    @Value("${keycloak.callback-url}")
    private String callbackUrl;
    @Value("${client.frontend.url}")
    private String clientUrl;
    @Value("${admin.frontend.url}")
    private String adminUrl;
    @Value("${app.jwt.refresh-token.expiration-second}")
    private long refreshTokenExpirationSecond;
    @Value("${app.jwt.access-token.expiration-second}")
    private long accessTokenExpirationSecond;
    @Value("${app.cookie.secure}")
    private boolean cookieSecure;
    @Value("${app.cookie.domain}")
    private String cookieDomain;

    @GetMapping("/callback")
    @AuditLog(action = "LOGIN", description = "Đăng nhập")
    public void callback(
            @RequestParam String code,
            @RequestParam String state,
            HttpServletResponse response
    ) throws Exception
    {
        LoginResponseCommand result = authenticationService.callback(code, callbackUrl);

        ResponseCookie accessCookie = createCookie("accessToken", result.accessToken(), accessTokenExpirationSecond);
        ResponseCookie refreshCookie = createCookie("refreshToken", result.refreshToken(), refreshTokenExpirationSecond);

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        String redirectUrl = state;
        if (result.user() != null && result.user().role() != null) {
            String roleName = result.user().role().getName();
            if ("ADMIN".equalsIgnoreCase(roleName)) {
                if (
                    redirectUrl == null || redirectUrl.isEmpty() || redirectUrl.contains(clientUrl)
                ) {
                    redirectUrl = adminUrl;
                }
            }
        }

        response.sendRedirect(redirectUrl);
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
}
