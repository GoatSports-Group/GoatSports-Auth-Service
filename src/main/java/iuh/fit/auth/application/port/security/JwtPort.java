package iuh.fit.auth.application.port.security;

import iuh.fit.auth.application.dto.AuthProperties;
import iuh.fit.auth.application.dto.response.auth.KeycloakParseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.domain.entity.User;

public interface JwtPort {
    String generateAccessToken(User user, String sessionId);

    String generateRefreshToken(User user, String sessionId);

    String getUserIdFromToken(String token);

    String getSessionIdFromToken(String token);

    String getRoleFromToken(String token);

    long getRemainingTime(String token);

    String hashPassword(String password);

    boolean matchesPassword(String rawPassword, String encodedPassword);

    KeycloakParseCommand decodeKeycloakToken(String token) throws ServiceException;

    AuthProperties getAuthProperties();
}
