package iuh.fit.auth.application.dto.response;

import java.util.UUID;

public record KeycloakInfoResponseCommand(
        UUID keycloakId,
        String email,
        String username,
        String fullName
) {}
