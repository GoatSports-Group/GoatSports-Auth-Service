package iuh.fit.auth.application.dto.request.role;

import java.util.Set;

public record UpdateRoleRequestCommand(
    String roleId,
    String name,
    String description,
    boolean active,
    Set<String> permissionIds
) {}
