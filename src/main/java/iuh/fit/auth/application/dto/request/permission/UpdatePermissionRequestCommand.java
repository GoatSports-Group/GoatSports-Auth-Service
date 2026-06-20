package iuh.fit.auth.application.dto.request.permission;

public record UpdatePermissionRequestCommand(
    String permissionId,
    String name,
    String apiPath,
    String method,
    String module
) {}
