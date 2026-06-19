package iuh.fit.auth.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KeycloakResponseCommand(
        @JsonProperty("access_token")
        String accessToken,
        @JsonProperty("refresh_token")
        String refreshToken,
        @JsonProperty("expires_in")
        Long expiresIn,
        @JsonProperty("refresh_expires_in")
        Long refreshExpiresIn
) {}
