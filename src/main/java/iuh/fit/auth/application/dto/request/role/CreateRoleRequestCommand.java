package iuh.fit.auth.application.dto.request.role;

public record CreateRoleRequestCommand(
    String name,
    String description
) {}
