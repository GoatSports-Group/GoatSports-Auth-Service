package iuh.fit.auth.application.dto.request.user;

public record CreateUserRequestCommand(
    String username,
    String fullName,
    String email,
    String password,
    String roleId
) {}
