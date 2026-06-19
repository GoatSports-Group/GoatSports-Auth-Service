package iuh.fit.auth.application.port.security;

import iuh.fit.auth.application.dto.response.KeycloakInfoResponseCommand;
import iuh.fit.auth.application.dto.response.KeycloakResponseCommand;

public interface KeycloakPort {
    KeycloakResponseCommand verify(String usernameOrEmail, String password);

    void logout(String refreshToken);

    KeycloakResponseCommand exchangeCode(String code, String redirectUri);

    KeycloakInfoResponseCommand decode(String accessToken);
}
