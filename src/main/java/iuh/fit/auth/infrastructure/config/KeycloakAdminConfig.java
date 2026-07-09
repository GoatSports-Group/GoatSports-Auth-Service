package iuh.fit.auth.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class KeycloakAdminConfig {

    private static final String DEFAULT_REALM_MASTER = "master";
    private static final String DEFAULT_CLIENT_ID = "admin-cli";

    @Value("${keycloak.server-url}")
    private String serverUrl;
    @Value("${keycloak.admin.username}")
    private String userName;
    @Value("${keycloak.admin.password}")
    private String password;
    @Value("${keycloak.realm-master}")
    private String realmMaster;
    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder
                .builder()
                .serverUrl(serverUrl)
                .realm(resolveRealmMaster())
                .clientId(resolveClientId())
                .grantType(OAuth2Constants.PASSWORD)
                .username(userName)
                .password(password)
                .build();
    }

    private String resolveRealmMaster() {
        if (realmMaster != null && !realmMaster.isBlank()) {
            return realmMaster.trim();
        }

        log.warn("keycloak.realm-master is blank. Falling back to {}", DEFAULT_REALM_MASTER);
        return DEFAULT_REALM_MASTER;
    }

    private String resolveClientId() {
        if (clientId != null && !clientId.isBlank()) {
            return clientId.trim();
        }

        log.warn("keycloak.client-id is blank. Falling back to {}", DEFAULT_CLIENT_ID);
        return DEFAULT_CLIENT_ID;
    }

}
