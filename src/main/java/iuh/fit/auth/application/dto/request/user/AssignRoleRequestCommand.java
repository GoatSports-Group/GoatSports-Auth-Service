package iuh.fit.auth.application.dto.request.user;

public record AssignRoleRequestCommand(
    String userId,
    String roleId
) {}
