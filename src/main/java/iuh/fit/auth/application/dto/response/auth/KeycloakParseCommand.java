package iuh.fit.auth.application.dto.response.auth;

import java.util.UUID;

public record KeycloakParseCommand(
        String email,
        String username,
        String fullName,
        UUID keycloakId
) {
}
