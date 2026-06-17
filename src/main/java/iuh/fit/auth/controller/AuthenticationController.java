package iuh.fit.auth.controller;

import iuh.fit.auth.dto.request.authentication.*;
import iuh.fit.auth.dto.response.authentication.UserResponse;
import iuh.fit.auth.entity.User;
import iuh.fit.auth.exception.ServiceException;
import iuh.fit.auth.service.AuthenticationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response, HttpServletRequest request
    ) throws ServiceException
    {
        UserResponse userResponse = authenticationService.login(loginRequest, response, request);
        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }

    @PostMapping("/login/keycloak")
    public ResponseEntity<UserResponse> loginWithKeycloak(
            @RequestBody LinkAccountRequest request, HttpServletResponse response
    ) throws ServiceException
    {
        UserResponse userResponse = authenticationService.loginWithKeycloak(request, response);
        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }

    @PostMapping("/link/keycloak")
    public ResponseEntity<Void> linkKeycloakAccount(@RequestBody LinkAccountRequest request) throws ServiceException {
        authenticationService.linkKeycloakAccount(request.getCode(), request.getRedirectUri());
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest registerRequest) throws ServiceException {
        UserResponse userResponse = authenticationService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyAccount(@RequestBody VerificationRequest verificationRequest)
            throws ServiceException {
        authenticationService.verifyUser(verificationRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend")
    public ResponseEntity<Void> resendVerificationCode(@RequestParam String email) throws ServiceException {
        authenticationService.resendVerificationCode(email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue("accessToken") String accessToken,
            @CookieValue("refreshToken") String refreshToken,
            HttpServletResponse response) {
        authenticationService.logout(accessToken, refreshToken, response);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @GetMapping("/refresh")
    public ResponseEntity<UserResponse> refresh(
            @CookieValue(name = "refreshToken", defaultValue = "missingValue") String refreshToken,
            HttpServletResponse response) throws ServiceException {
        UserResponse userResponse = authenticationService.refresh(refreshToken, response);
        return ResponseEntity.status(HttpStatus.OK).body(userResponse);
    }

    @PutMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest forgotPasswordRequest)
            throws ServiceException {
        authenticationService.forgotPassword(forgotPasswordRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() throws ServiceException {
        User user = authenticationService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.OK).body(authenticationService.convertToResponse(user));
    }
}
