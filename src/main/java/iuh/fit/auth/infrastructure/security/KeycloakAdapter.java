package iuh.fit.auth.infrastructure.security;

import iuh.fit.auth.application.dto.response.auth.KeycloakResponseCommand;
import iuh.fit.auth.application.exception.ServiceException;
import iuh.fit.auth.application.port.security.KeycloakPort;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdapter implements KeycloakPort {

    private final Keycloak keycloak;
    private final RestClient restClient = RestClient.create();

    @Value("${keycloak.token-uri}")
    private String tokenUri;
    @Value("${keycloak.logout-uri}")
    private String logoutUri;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.current-realm}")
    private String currentRealm;

    @Override
    public KeycloakResponseCommand verify(String usernameOrEmail, String password) {
        try {
            KeycloakResponseCommand response = restClient
                    .post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=password"
                            + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
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
                        + "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8))
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public KeycloakResponseCommand refreshKeycloakToken(String keycloakRefreshToken) {
        try {
            String body = "grant_type=refresh_token"
                    + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
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

    @Override
    public void updatePassword(String userId, String newPassword) throws ServiceException {
        try {
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(newPassword);
            credential.setTemporary(false);

            keycloak.realm(currentRealm)
                    .users()
                    .get(userId)
                    .resetPassword(credential);

            UserRepresentation userRep = keycloak.realm(currentRealm)
                    .users()
                    .get(userId)
                    .toRepresentation();
            userRep.setEnabled(false);
            userRep.setEmailVerified(false);

            keycloak.realm(currentRealm)
                    .users()
                    .get(userId)
                    .update(userRep);

            log.info("Successfully updated Keycloak password and disabled user ID: {}", userId);
        } catch (BadRequestException e) {
            log.error("Password policy violation in Keycloak during forgotPassword for user {}: {}", userId, e.getMessage());
            throw new ServiceException("Mật khẩu không đạt chuẩn chính sách bảo mật!");
        } catch (Exception e) {
            log.error("Failed to update password in Keycloak during forgotPassword: {}", e.getMessage(), e);
            throw new ServiceException("Không thể cập nhật mật khẩu trên hệ thống xác thực.");
        }
    }

    @Override
    public void deleteUser(java.util.UUID keycloakId) throws ServiceException {
        try (jakarta.ws.rs.core.Response response = keycloak.realm(currentRealm).users().delete(keycloakId.toString())) {
            int status = response.getStatus();
            if (status != 204 && status != 404) {
                throw new ServiceException("Không xóa được tài khoản trên hệ thống xác thực (HTTP " + status + ").");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete Keycloak user {}: {}", keycloakId, e.getMessage());
            throw new ServiceException("Không xóa được tài khoản trên hệ thống xác thực.");
        }
    }

    @Override
    public KeycloakResponseCommand exchangeCode(String code, String redirectUri) throws ServiceException {
        try {
            return restClient
                    .post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=authorization_code"
                            + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                            + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                            + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8))
                    .retrieve()
                    .body(KeycloakResponseCommand.class);
        } catch (RestClientResponseException e) {
            log.error("Failed to exchange Keycloak code: {}", e.getResponseBodyAsString());
            throw new ServiceException("Exchange Keycloak code failed: " + e.getMessage());
        }
    }
}
