package iuh.fit.auth.service.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import iuh.fit.auth.service.KeycloakService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import iuh.fit.auth.dto.response.authentication.KeycloakResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakServiceImpl implements KeycloakService {

    private final RestClient restClient = RestClient.create();

    @Value("${keycloak.token-uri}")
    private String tokenUri;
    @Value("${keycloak.logout-uri}")
    private String logoutUri;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Override
    public KeycloakResponse verify(String usernameOrEmail, String password) {
        try {
            KeycloakResponse response = restClient
                    .post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=password"
                            + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                            + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                            + "&username=" + URLEncoder.encode(usernameOrEmail, StandardCharsets.UTF_8)
                            + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8))
                    .retrieve()
                    .body(KeycloakResponse.class);

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
    public KeycloakResponse exchangeCode(String code,String redirectUri) {
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
                .body(KeycloakResponse.class);
    }
}
