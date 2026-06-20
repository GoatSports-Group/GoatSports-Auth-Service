package iuh.fit.auth.application.dto.request.permission;

public record CreatePermissionRequestCommand(
    String name,
    String apiPath,
    String method,
    String module
) {}
