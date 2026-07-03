package iuh.fit.auth.infrastructure.security;

import iuh.fit.auth.application.dto.response.auth.KeycloakInfoResponseCommand;
import iuh.fit.auth.application.dto.response.auth.KeycloakResponseCommand;
import iuh.fit.auth.application.port.security.KeycloakPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdapter implements KeycloakPort {

    private final RestClient restClient = RestClient.create();
    private final JwtDecoder keycloakJwtDecoder;

    @Value("${keycloak.token-uri}")
    private String tokenUri;
    @Value("${keycloak.logout-uri}")
    private String logoutUri;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Override
    public KeycloakResponseCommand verify(String usernameOrEmail, String password) {
        try {
            KeycloakResponseCommand response = restClient
                    .post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=password"
                            + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                            + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                            + "&username=" + URLEncoder.encode(usernameOrEmail, StandardCharsets.UTF_8)
                            + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8))
                    .retrieve()
                    .body(KeycloakResponseCommand.class);

            log.debug("Password verified successfully for user: {}", usernameOrEmail);
            return response;
        } catch (RestClientResponseException e) {
            log.error("Password verification failed for user: {}", usernameOrEmail);
            log.error("Keycloak status: {}", e.getStatusCode());
            log.error("Keycloak response: {}", e.getResponseBodyAsString());
            return null;
        }
    }

    @Override
    public void logout(String refreshToken) {
        restClient
                .post()
                .uri(logoutUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                        + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                        + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public KeycloakResponseCommand exchangeCode(String code, String redirectUri) {
        String body = "grant_type=authorization_code"
                + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        return restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(KeycloakResponseCommand.class);
    }

    @Override
    public KeycloakInfoResponseCommand decode(String accessToken) {
        Jwt jwt = keycloakJwtDecoder.decode(accessToken);

        UUID keycloakId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("preferred_username");
        String fullName = jwt.getClaimAsString("name");

        return new KeycloakInfoResponseCommand(keycloakId, email, username, fullName);
    }

    @Override
    public KeycloakResponseCommand refreshKeycloakToken(String keycloakRefreshToken) {
        try {
            String body = "grant_type=refresh_token"
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&refresh_token=" + URLEncoder.encode(keycloakRefreshToken, StandardCharsets.UTF_8);

            return restClient.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(KeycloakResponseCommand.class);
        } catch (Exception e) {
            return null;
        }
    }
}
