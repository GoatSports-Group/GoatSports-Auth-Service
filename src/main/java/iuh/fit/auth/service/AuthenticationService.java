package iuh.fit.auth.service;

import iuh.fit.auth.dto.request.authentication.*;
import iuh.fit.auth.dto.response.authentication.UserResponse;
import iuh.fit.auth.entity.User;
import iuh.fit.auth.exception.ServiceException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthenticationService {
    UserResponse login(LoginRequest loginRequest, HttpServletResponse response, HttpServletRequest request)
            throws ServiceException;

    UserResponse loginWithKeycloak(LinkAccountRequest request, HttpServletResponse response) throws ServiceException;

    void linkKeycloakAccount(String code, String redirectUri) throws ServiceException;

    UserResponse register(RegisterRequest request) throws ServiceException;

    void verifyUser(VerificationRequest verificationRequest) throws ServiceException;

    void resendVerificationCode(String email) throws ServiceException;

    void logout(String accessToken, String refreshToken, HttpServletResponse response);

    UserResponse refresh(String refreshToken, HttpServletResponse response) throws ServiceException;

    void forgotPassword(ForgotPasswordRequest forgotPasswordRequest) throws ServiceException;

    User getCurrentUser() throws ServiceException;

    UserResponse convertToResponse(User user);
}