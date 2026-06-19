package iuh.fit.auth.application.port.security;

import iuh.fit.auth.application.dto.AuthProperties;
import iuh.fit.auth.domain.entity.User;

public interface JwtPort {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    String getUserIdFromToken(String token);
    String getRoleFromToken(String token);
    long getRemainingTime(String token);
    String hashPassword(String password);
    AuthProperties getAuthProperties();
}
