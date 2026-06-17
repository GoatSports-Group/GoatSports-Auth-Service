package iuh.fit.auth.service;

import iuh.fit.auth.dto.response.authentication.KeycloakResponse;

public interface KeycloakService {
    KeycloakResponse verify(String usernameOrEmail, String password);

    void logout(String refreshToken);

    KeycloakResponse exchangeCode(String code, String redirectUri);
}
