package iuh.fit.auth.presentation.controller;

import iuh.fit.auth.application.dto.request.auth.*;
import iuh.fit.auth.application.usecase.auth.AuthenticationUseCase;
import iuh.fit.auth.domain.entity.User;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.presentation.converter.UserConverter;
import iuh.fit.auth.presentation.request.auth.*;
import iuh.fit.auth.presentation.response.user.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final AuthenticationUseCase authenticationUseCase;

    @Value("${app.jwt.refresh-token.expiration-second}")
    private long refreshTokenExpirationSecond;
    @Value("${app.jwt.access-token.expiration-second}")
    private long accessTokenExpirationSecond;
    @Value("${app.cookie.domain}")
    private String cookieDomain;
    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response
    ) throws ServiceException
    {
        Authentication authentication = authenticationManagerBuilder.getObject()
                .authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginRequest.getUsername(),
                                loginRequest.getPassword()
                        )
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Map<String, Object> result = authenticationUseCase.login(
                new LoginRequestCommand(loginRequest.getUsername(), loginRequest.getPassword())
        );

        return setCookie(response, result);
    }

    @PostMapping("/login/keycloak")
    public ResponseEntity<UserResponse> loginWithKeycloak(
            @RequestBody LinkAccountRequest request, HttpServletResponse response
    ) throws ServiceException {
        Map<String, Object> result = authenticationUseCase.loginWithKeycloak(
                new LinkAccountRequestCommand(request.getCode(), request.getRedirectUri())
        );

        return setCookie(response, result);
    }

    @PostMapping("/link/keycloak")
    @Transactional
    public ResponseEntity<Void> linkKeycloakAccount(@RequestBody LinkAccountRequest request) throws ServiceException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ServiceException("Bạn cần đăng nhập trước khi liên kết tài khoản");
        }

        authenticationUseCase.linkKeycloakAccount(
                new LinkAccountRequestCommand(request.getCode(), request.getRedirectUri())
        );

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest registerRequest) throws ServiceException {
        User userResponse = authenticationUseCase.register(
                new RegisterRequestCommand(
                        registerRequest.getUsername(), registerRequest.getFullName(), registerRequest.getEmail(),
                        registerRequest.getPassword(), registerRequest.getConfirmPassword()
                )
        );
        return ResponseEntity.status(HttpStatus.OK).body(UserConverter.toResponse(userResponse));
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyAccount(@RequestBody VerificationRequest verificationRequest)
            throws ServiceException {
        authenticationUseCase.verifyUser(
                new VerificationRequestCommand(verificationRequest.getEmail(), verificationRequest.getVerificationCode())
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend")
    public ResponseEntity<Void> resendVerificationCode(@RequestParam String email) throws ServiceException {
        authenticationUseCase.resendVerificationCode(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue("accessToken") String accessToken,
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response
    ){
        authenticationUseCase.logout(accessToken, refreshToken);

        ResponseCookie deleteAccessCookie = createCookie("accessToken", "", 0);
        ResponseCookie deleteRefreshCookie = createCookie("refreshToken", "", 0);

        response.addHeader(HttpHeaders.SET_COOKIE, deleteAccessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefreshCookie.toString());

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/refresh")
    public ResponseEntity<UserResponse> refresh(
            @CookieValue(name = "refreshToken", defaultValue = "missingValue") String refreshToken,
            HttpServletResponse response
    ) throws ServiceException
    {
        Map<String, Object> result = authenticationUseCase.refresh(refreshToken);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                result.get("user"), null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + result.get("role").toString()))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return setCookie(response, result);
    }

    @PutMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest
    ) throws ServiceException
    {
        authenticationUseCase.forgotPassword(
                new ForgotPasswordRequestCommand(
                        forgotPasswordRequest.getEmail(),
                        forgotPasswordRequest.getPassword(), forgotPasswordRequest.getConfirmPassword()
                )
        );

        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() throws ServiceException {
        User user = authenticationUseCase.getCurrentUser();
        return ResponseEntity.status(HttpStatus.OK).body(UserConverter.toResponse(user));
    }

    private ResponseCookie createCookie(String name, String value, long maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge);
        return builder.build();
    }

    private ResponseEntity<UserResponse> setCookie(HttpServletResponse response, Map<String, Object> result) {
        ResponseCookie accessCookie = createCookie("accessToken", result.get("accessToken").toString(), accessTokenExpirationSecond);
        ResponseCookie refreshCookie = createCookie("refreshToken", result.get("refreshToken").toString(), refreshTokenExpirationSecond);

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.status(HttpStatus.OK)
                .body(result.get("user") != null ? UserConverter.toResponse((User) result.get("user")) : null);
    }
}
