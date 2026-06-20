package iuh.fit.auth.application.dto.response.permission;

import java.util.UUID;

public record PermissionResponseCommand(
        UUID permissionId,
        String name,
        String apiPath,
        String method,
        String module
) {}
