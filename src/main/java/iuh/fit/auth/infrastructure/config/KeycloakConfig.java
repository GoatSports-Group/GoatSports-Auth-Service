package iuh.fit.auth.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

@Configuration
public class KeycloakConfig {
    @Value("${keycloak.user-uri}")
    private String issuerUri;

    @Bean
    public JwtDecoder keycloakJwtDecoder() {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }
}
