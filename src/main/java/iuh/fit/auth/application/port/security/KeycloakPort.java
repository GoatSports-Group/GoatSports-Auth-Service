package iuh.fit.auth.application.port.security;

import iuh.fit.auth.application.dto.response.auth.KeycloakResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;

import java.util.UUID;

public interface KeycloakPort {
    KeycloakResponseCommand verify(String usernameOrEmail, String password);

    void logout(String refreshToken);

    KeycloakResponseCommand refreshKeycloakToken(String keycloakRefreshToken);

    void updatePassword(String userId, String newPassword) throws ServiceException;

    KeycloakResponseCommand exchangeCode(String code, String redirectUri) throws ServiceException;

    void deleteUser(UUID keycloakId) throws ServiceException;
}
