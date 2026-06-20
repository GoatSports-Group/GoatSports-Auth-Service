package iuh.fit.auth.application.dto.response.user;

import java.util.Set;

public record UserPermissionsResponseCommand(
    String email,
    String role,
    Set<String> permissions
) {}
