package iuh.fit.auth.application.dto.response.role;

import iuh.fit.auth.application.dto.response.permission.PermissionResponseCommand;

import java.util.Set;
import java.util.UUID;

public record RoleResponseCommand(
        UUID roleId,
        String name,
        String description,
        boolean active,
        Set<PermissionResponseCommand> permissions
) {}
